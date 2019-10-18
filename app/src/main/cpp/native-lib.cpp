#include <jni.h>
#include <opencv2/opencv.hpp>
#include <opencv2/aruco.hpp>
#include <android/log.h>
#include <android/bitmap.h>
#include <android/native_window.h>
#include <android/native_window_jni.h>

#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgproc/imgproc_c.h>
#include <opencv2/features2d/features2d.hpp>

#include <algorithm>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <math.h>

using namespace std;
using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_arucodetection_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                              jint src_width, jint src_height,
                                                              jobject buffer, jobject dst) {
    // TODO: implement ConvertRGBtoGray()
    uint8_t *srcLumaPtr = reinterpret_cast<uint8_t *>(env->GetDirectBufferAddress(buffer));

    int dstWidth;
    int dstHeight;

    cv::Mat mYuv(src_height + src_height / 2, src_width, CV_8UC1, srcLumaPtr);

    ANativeWindow *win = ANativeWindow_fromSurface(env, dst);
    ANativeWindow_acquire(win);

    ANativeWindow_Buffer buf;

    dstWidth = src_height;
    dstHeight = src_width;

    ANativeWindow_setBuffersGeometry(win, dstWidth, dstHeight, 0 /*format unchanged*/);

    if (int32_t err = ANativeWindow_lock(win, &buf, NULL)) {
        ANativeWindow_release(win);
        return;
    }

    uint8_t *dstLumaPtr = reinterpret_cast<uint8_t *>(buf.bits);
    Mat dstRgba(dstHeight, buf.stride, CV_8UC4,
                dstLumaPtr);        // TextureView buffer, use stride as width
    Mat srcRgba(src_height, src_width, CV_8UC4);
    Mat flipRgba(dstHeight, dstWidth, CV_8UC4);
    Mat tempGray(src_height, src_width, CV_8UC4);

    cv::cvtColor(mYuv, srcRgba, COLOR_YUV2RGBA_NV21);

    //cvtColor(tempGray, srcRgba, COLOR_RGBA2GRAY);

    // Rotate 90 degree
    cv::transpose(srcRgba, flipRgba);
    cv::flip(flipRgba, flipRgba, 1);

    uchar *dbuf;
    uchar *sbuf;
    dbuf = dstRgba.data;
    sbuf = flipRgba.data;
    int i;
    for (i = 0; i < flipRgba.rows; i++) {
        dbuf = dstRgba.data + i * buf.stride * 4;
        memcpy(dbuf, sbuf, flipRgba.cols * 4);
        sbuf += flipRgba.cols * 4;
    }

    ANativeWindow_unlockAndPost(win);
    ANativeWindow_release(win);
}