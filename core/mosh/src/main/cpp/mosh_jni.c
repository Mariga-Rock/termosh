#include <jni.h>
#include <android/log.h>
#include <errno.h>
#include <fcntl.h>
#include <signal.h>
#include <stdlib.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>

#define LOG_TAG "MoshPty"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)

static int create_pty_subprocess(char* const argv[], char* const envp[],
                                 const char* cwd, int rows, int cols,
                                 int* out_pid) {
    int ptm = open("/dev/ptmx", O_RDWR | O_CLOEXEC);
    if (ptm < 0) { LOGE("open /dev/ptmx: %s", strerror(errno)); return -1; }
    if (grantpt(ptm) < 0) { LOGE("grantpt: %s", strerror(errno)); close(ptm); return -1; }
    if (unlockpt(ptm) < 0) { LOGE("unlockpt: %s", strerror(errno)); close(ptm); return -1; }

    char devname[128];
    if (ptsname_r(ptm, devname, sizeof(devname)) != 0) {
        LOGE("ptsname_r: %s", strerror(errno)); close(ptm); return -1;
    }

    int pts = open(devname, O_RDWR | O_NOCTTY);
    if (pts < 0) { LOGE("open pts %s: %s", devname, strerror(errno)); close(ptm); return -1; }

    struct winsize sz = { 0 };
    sz.ws_row = (unsigned short)rows;
    sz.ws_col = (unsigned short)cols;
    ioctl(ptm, TIOCSWINSZ, &sz);

    pid_t pid = fork();
    if (pid < 0) { LOGE("fork: %s", strerror(errno)); close(pts); close(ptm); return -1; }

    if (pid == 0) {
        setsid();
        ioctl(pts, TIOCSCTTY, 0);
        dup2(pts, 0); dup2(pts, 1); dup2(pts, 2);
        if (pts > 2) close(pts);
        close(ptm);

        if (cwd) chdir(cwd);

        if (envp) execve(argv[0], argv, envp);
        else      execv(argv[0], argv);
        LOGE("execve(%s) failed: %s", argv[0], strerror(errno));
        _exit(127);
    }

    close(pts);
    *out_pid = (int)pid;
    return ptm;
}

JNIEXPORT jintArray JNICALL
Java_app_termosh_core_mosh_MoshPty_nativeSpawn(
    JNIEnv* env, jclass clazz,
    jobjectArray jargv, jobjectArray jenvp,
    jstring jcwd, jint rows, jint cols)
{
    int argc = (*env)->GetArrayLength(env, jargv);
    char** argv = (char**)calloc((size_t)argc + 1, sizeof(char*));
    for (int i = 0; i < argc; i++) {
        jstring s = (jstring)(*env)->GetObjectArrayElement(env, jargv, i);
        const char* cs = (*env)->GetStringUTFChars(env, s, NULL);
        argv[i] = strdup(cs);
        (*env)->ReleaseStringUTFChars(env, s, cs);
        (*env)->DeleteLocalRef(env, s);
    }
    argv[argc] = NULL;

    int envc = jenvp ? (*env)->GetArrayLength(env, jenvp) : 0;
    char** envp = NULL;
    if (envc > 0) {
        envp = (char**)calloc((size_t)envc + 1, sizeof(char*));
        for (int i = 0; i < envc; i++) {
            jstring s = (jstring)(*env)->GetObjectArrayElement(env, jenvp, i);
            const char* cs = (*env)->GetStringUTFChars(env, s, NULL);
            envp[i] = strdup(cs);
            (*env)->ReleaseStringUTFChars(env, s, cs);
            (*env)->DeleteLocalRef(env, s);
        }
        envp[envc] = NULL;
    }

    const char* cwd = NULL;
    if (jcwd) cwd = (*env)->GetStringUTFChars(env, jcwd, NULL);

    int pid = -1;
    int ptm = create_pty_subprocess(argv, envp, cwd, (int)rows, (int)cols, &pid);

    for (int i = 0; i < argc; i++) free(argv[i]);
    free(argv);
    if (envp) { for (int i = 0; i < envc; i++) free(envp[i]); free(envp); }
    if (cwd) (*env)->ReleaseStringUTFChars(env, jcwd, cwd);

    jint vals[2] = { ptm, pid };
    jintArray out = (*env)->NewIntArray(env, 2);
    (*env)->SetIntArrayRegion(env, out, 0, 2, vals);
    return out;
}

JNIEXPORT void JNICALL
Java_app_termosh_core_mosh_MoshPty_nativeResize(
    JNIEnv* env, jclass clazz, jint fd, jint rows, jint cols)
{
    struct winsize sz = { 0 };
    sz.ws_row = (unsigned short)rows;
    sz.ws_col = (unsigned short)cols;
    ioctl((int)fd, TIOCSWINSZ, &sz);
}

JNIEXPORT jint JNICALL
Java_app_termosh_core_mosh_MoshPty_nativeWaitFor(
    JNIEnv* env, jclass clazz, jint pid)
{
    int status = 0;
    if (waitpid((pid_t)pid, &status, 0) < 0) return -1;
    if (WIFEXITED(status))  return WEXITSTATUS(status);
    if (WIFSIGNALED(status)) return 128 + WTERMSIG(status);
    return -1;
}

JNIEXPORT jboolean JNICALL
Java_app_termosh_core_mosh_MoshPty_nativeIsAlive(
    JNIEnv* env, jclass clazz, jint pid)
{
    if (kill((pid_t)pid, 0) == 0) return JNI_TRUE;
    if (errno == EPERM) return JNI_TRUE;
    return JNI_FALSE;
}
