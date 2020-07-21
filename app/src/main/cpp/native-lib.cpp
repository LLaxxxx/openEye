#include <jni.h>
#include <string>
#include <opencv2/core.hpp>
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/objdetect.hpp>
#include <opencv2/features2d.hpp>
#include <vector>
#include <android/bitmap.h>
#include <android/log.h>
#include <iostream>
#include <random>
#include <math.h>

using namespace cv;
using namespace std;

#define ASSERT(status, ret)     if (!(status)) { return ret; }
#define ASSERT_FALSE(status)    ASSERT(status, false)

CascadeClassifier cascadeClassifier;
vector<Rect> eyes;
int aveWidth = 45, aveY = 30;
int eyenum = 0;

bool BitmapToMat(JNIEnv *env, jobject obj_bitmap, cv::Mat &matrix) {
    void *bitmapPixels;                                            // 保存图片像素数据
    AndroidBitmapInfo bitmapInfo;                                   // 保存图片参数

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >= 0);        // 获取图片参数
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888|| bitmapInfo.format ==ANDROID_BITMAP_FORMAT_RGB_565);          // 只支持 ARGB_8888 和 RGB_565
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >= 0);  // 获取图片像素（锁定内存块）
    ASSERT_FALSE(bitmapPixels);

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);    // 建立临时 mat
        tmp.copyTo(matrix);                                                         // 拷贝到目标 matrix
    } else {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        cv::cvtColor(tmp, matrix, cv::COLOR_BGR5652RGB);
    }

    AndroidBitmap_unlockPixels(env, obj_bitmap);            // 解锁
    return true;
}
bool MatToBitmap(JNIEnv *env, cv::Mat &matrix, jobject obj_bitmap) {
    void *bitmapPixels;                                            // 保存图片像素数据
    AndroidBitmapInfo bitmapInfo;                                   // 保存图片参数

    ASSERT_FALSE(AndroidBitmap_getInfo(env, obj_bitmap, &bitmapInfo) >= 0);        // 获取图片参数
    ASSERT_FALSE(bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888
                 || bitmapInfo.format ==
                    ANDROID_BITMAP_FORMAT_RGB_565);          // 只支持 ARGB_8888 和 RGB_565
    ASSERT_FALSE(matrix.dims == 2
                 && bitmapInfo.height == (uint32_t) matrix.rows
                 && bitmapInfo.width == (uint32_t) matrix.cols);                   // 必须是 2 维矩阵，长宽一致
    ASSERT_FALSE(matrix.type() == CV_8UC1 || matrix.type() == CV_8UC3 || matrix.type() == CV_8UC4);
    ASSERT_FALSE(AndroidBitmap_lockPixels(env, obj_bitmap, &bitmapPixels) >= 0);  // 获取图片像素（锁定内存块）
    ASSERT_FALSE(bitmapPixels);

    if (bitmapInfo.format == ANDROID_BITMAP_FORMAT_RGBA_8888) {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC4, bitmapPixels);
        switch (matrix.type()) {
            case CV_8UC1:
                cv::cvtColor(matrix, tmp, cv::COLOR_GRAY2RGBA);
                break;
            case CV_8UC3:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGB2RGBA);
                break;
            case CV_8UC4:
                matrix.copyTo(tmp);
                break;
            default:
                AndroidBitmap_unlockPixels(env, obj_bitmap);
                return false;
        }
    } else {
        cv::Mat tmp(bitmapInfo.height, bitmapInfo.width, CV_8UC2, bitmapPixels);
        switch (matrix.type()) {
            case CV_8UC1:
                cv::cvtColor(matrix, tmp, cv::COLOR_GRAY2BGR565);
                break;
            case CV_8UC3:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGB2BGR565);
                break;
            case CV_8UC4:
                cv::cvtColor(matrix, tmp, cv::COLOR_RGBA2BGR565);
                break;
            default:
                AndroidBitmap_unlockPixels(env, obj_bitmap);
                return false;
        }
    }
    AndroidBitmap_unlockPixels(env, obj_bitmap);                // 解锁
    return true;
}


extern "C"
JNIEXPORT jobject JNICALL
Java_com_example_opencvappeye_MainActivity_detectEyes(JNIEnv *env, jclass clazz, jobject bitmap) {
    // TODO: implement detectEyes()
    Mat src;
    eyes.clear();
    BitmapToMat(env, bitmap, src);
    Mat src2;
    //处理灰度图, 提高效率
    cvtColor(src, src2, COLOR_BGRA2GRAY);
    equalizeHist(src2, src2);
    cascadeClassifier.detectMultiScale(src2, eyes, 1.1, 2, 0);

    for (Rect eyeRect : eyes)
        rectangle(src, eyeRect, Scalar(255, 155, 155), 1);
    MatToBitmap(env, src, bitmap);
    return bitmap;
}

extern "C"
JNIEXPORT void JNICALL
Java_com_example_opencvappeye_MainActivity_loadCacade(JNIEnv *env, jclass clazz,
                                                      jstring file_path) {
    // TODO: implement loadCacade()
    const char *filePath = env->GetStringUTFChars(file_path, 0);
    cascadeClassifier.load(filePath);
    env->ReleaseStringUTFChars(file_path, filePath);
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_opencvappeye_MainActivity_blink(JNIEnv *env, jclass clazz, jobject bitmap) {
    // TODO: implement blink()
    Mat src;
    eyes.clear();
    BitmapToMat(env, bitmap, src);
    cvtColor(src, src, COLOR_BGRA2GRAY);
    equalizeHist(src, src);
/*
    1.image表示的是要检测的输入图像
    2.objects表示检测到的人脸目标序列
    3.scaleFactor表示每次图像尺寸减小的比例
    4. minNeighbors表示每一个目标至少要被检测到3次才算是真的目标(因为周围的像素和不同的窗口大小都可以检测到人脸),
    5. falgs
    6.minSize为目标的最小尺寸
    7.minSize为目标的最大尺寸
*/
    cascadeClassifier.detectMultiScale(src, eyes, 1.1, 2, 0,
            Size(aveWidth-20,aveWidth-20), Size(aveWidth +20,aveWidth+20));
    if (eyes.size() != eyenum){
        eyenum = eyes.size();
        return static_cast<jboolean>(true);
    }
    return static_cast<jboolean>(false);
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_example_opencvappeye_MainActivity_calibration(JNIEnv *env, jclass clazz, jint num,
                                                       jint size) {
    // TODO: implement calibration()
    if(eyes.size() == num){
        for (Rect eyeRect : eyes){
            if(eyeRect.width > size + 10 || eyeRect.width < size - 10){
                return static_cast<jboolean>(false);
            }
            aveWidth += eyeRect.width/2;
            aveY += eyeRect.y/2;
        }
        int diffY = eyes.at(0).y - eyes.at(1).y;
        return static_cast<jboolean>(!(diffY > 20 || diffY < -20));
    }
    return static_cast<jboolean>(false);
}