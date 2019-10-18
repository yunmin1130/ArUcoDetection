#include <jni.h>
#include <opencv2/opencv.hpp>

using namespace cv;

extern "C"
JNIEXPORT void JNICALL
Java_com_example_arucodetection_MainActivity_ConvertRGBtoGray(JNIEnv *env, jobject thiz,
                                                                      jlong matAddrInput,
                                                                      jlong matAddrResult) {
    // TODO: implement ConvertRGBtoGray()
    Mat &matInput = *(Mat *)matAddrInput;
    Mat &matResult = *(Mat *)matAddrResult;

    cvtColor(matInput, matResult, COLOR_RGBA2GRAY);
}