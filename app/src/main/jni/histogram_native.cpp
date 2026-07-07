#include <android/bitmap.h>
#include <jni.h>

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstdint>
#include <limits>
#include <thread>
#include <vector>

namespace {

using Clock = std::chrono::steady_clock;
using Histogram = std::array<int32_t, 256>;

constexpr int kPreGrayscale = 0;
constexpr int kWhileCounting = 1;
constexpr int kFixedScale = 1000;
constexpr int kHalfScale = 500;

int64_t nanosBetween(const Clock::time_point& start, const Clock::time_point& end) {
    return std::chrono::duration_cast<std::chrono::nanoseconds>(end - start).count();
}

void throwIllegalArgument(JNIEnv* env, const char* message) {
    jclass exceptionClass = env->FindClass("java/lang/IllegalArgumentException");
    if (exceptionClass != nullptr) {
        env->ThrowNew(exceptionClass, message);
    }
}

inline uint8_t floatingPointGray(uint8_t red, uint8_t green, uint8_t blue) {
    double value = static_cast<double>(red) * 0.299;
    value += static_cast<double>(green) * 0.587;
    value += static_cast<double>(blue) * 0.114;
    return static_cast<uint8_t>(std::floor(value + 0.5));
}

inline uint8_t grayscale(uint8_t red, uint8_t green, uint8_t blue) {
    const int weighted = red * 299 + green * 587 + blue * 114;
    const int base = weighted / kFixedScale;
    const int remainder = weighted - base * kFixedScale;
    if (remainder == kHalfScale) {
        return floatingPointGray(red, green, blue);
    }
    return static_cast<uint8_t>(base + (remainder > kHalfScale ? 1 : 0));
}

inline uint8_t unpremultiply(uint8_t component, uint8_t alpha) {
    if (alpha == 0 || alpha == 255) {
        return component;
    }
    return static_cast<uint8_t>(std::min(255, (component * 255 + alpha / 2) / alpha));
}

struct PixelView {
    const uint8_t* pixels;
    uint32_t width;
    uint32_t height;
    uint32_t stride;
    bool premultiplied;
};

inline uint8_t grayAt(const PixelView& view, uint32_t x, uint32_t y) {
    const uint8_t* rgba = view.pixels + static_cast<size_t>(y) * view.stride + x * 4;
    uint8_t red = rgba[0];
    uint8_t green = rgba[1];
    uint8_t blue = rgba[2];
    if (view.premultiplied) {
        const uint8_t alpha = rgba[3];
        red = unpremultiply(red, alpha);
        green = unpremultiply(green, alpha);
        blue = unpremultiply(blue, alpha);
    }
    return grayscale(red, green, blue);
}

int chooseWorkerCount(int requested, uint32_t height) {
    const int hardware = static_cast<int>(std::max(1u, std::thread::hardware_concurrency()));
    const int boundedRequest = requested > 0 ? requested : hardware;
    // 部分 Android/模拟器的 hardware_concurrency() 会返回 1；优先采用 JVM 已探测的请求值。
    return std::max(1, std::min({boundedRequest, 8, static_cast<int>(height)}));
}

template <typename Work>
void runRows(int workers, uint32_t height, Work work) {
    std::vector<std::thread> threads;
    threads.reserve(workers);
    for (int worker = 0; worker < workers; ++worker) {
        const uint32_t startRow = static_cast<uint32_t>(
            (static_cast<uint64_t>(height) * worker) / workers
        );
        const uint32_t endRow = static_cast<uint32_t>(
            (static_cast<uint64_t>(height) * (worker + 1)) / workers
        );
        threads.emplace_back([=, &work]() { work(worker, startRow, endRow); });
    }
    for (auto& thread : threads) {
        thread.join();
    }
}

}  // namespace

extern "C" JNIEXPORT jintArray JNICALL
Java_com_lzx_imagehistogramanalyzer_data_image_NativeHistogramBridge_nativeCalculate(
    JNIEnv* env,
    jobject,
    jobject bitmap,
    jint strategy,
    jint requestedThreads,
    jlongArray timingsArray
) {
    if (bitmap == nullptr || timingsArray == nullptr || env->GetArrayLength(timingsArray) < 6) {
        throwIllegalArgument(env, "Native 直方图参数无效");
        return nullptr;
    }
    if (strategy != kPreGrayscale && strategy != kWhileCounting) {
        throwIllegalArgument(env, "Native 直方图方案无效");
        return nullptr;
    }

    const auto totalStart = Clock::now();
    AndroidBitmapInfo info{};
    if (AndroidBitmap_getInfo(env, bitmap, &info) != ANDROID_BITMAP_RESULT_SUCCESS ||
        info.width == 0 || info.height == 0 ||
        info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        throwIllegalArgument(env, "Native 仅支持软件 RGBA_8888 Bitmap");
        return nullptr;
    }

    void* rawPixels = nullptr;
    const auto lockStart = Clock::now();
    if (AndroidBitmap_lockPixels(env, bitmap, &rawPixels) != ANDROID_BITMAP_RESULT_SUCCESS ||
        rawPixels == nullptr) {
        throwIllegalArgument(env, "Native 无法锁定 Bitmap 像素");
        return nullptr;
    }
    const auto lockEnd = Clock::now();

    const int workers = chooseWorkerCount(requestedThreads, info.height);
    const PixelView view{
        static_cast<const uint8_t*>(rawPixels),
        info.width,
        info.height,
        info.stride,
        (info.flags & ANDROID_BITMAP_FLAGS_ALPHA_MASK) == ANDROID_BITMAP_FLAGS_ALPHA_PREMUL,
    };
    const size_t pixelCount = static_cast<size_t>(info.width) * info.height;
    std::vector<Histogram> localHistograms(workers);
    for (auto& histogram : localHistograms) {
        histogram.fill(0);
    }

    int64_t grayscaleNanos = 0;
    int64_t countingNanos = 0;

    if (strategy == kPreGrayscale) {
        std::vector<uint8_t> grayscalePixels(pixelCount);
        const auto grayscaleStart = Clock::now();
        runRows(workers, info.height, [&](int, uint32_t startRow, uint32_t endRow) {
            for (uint32_t y = startRow; y < endRow; ++y) {
                const size_t rowOffset = static_cast<size_t>(y) * info.width;
                for (uint32_t x = 0; x < info.width; ++x) {
                    grayscalePixels[rowOffset + x] = grayAt(view, x, y);
                }
            }
        });
        grayscaleNanos = nanosBetween(grayscaleStart, Clock::now());

        const auto countingStart = Clock::now();
        runRows(workers, info.height, [&](int worker, uint32_t startRow, uint32_t endRow) {
            Histogram& local = localHistograms[worker];
            const size_t start = static_cast<size_t>(startRow) * info.width;
            const size_t end = static_cast<size_t>(endRow) * info.width;
            for (size_t index = start; index < end; ++index) {
                ++local[grayscalePixels[index]];
            }
        });
        countingNanos = nanosBetween(countingStart, Clock::now());
    } else {
        const auto countingStart = Clock::now();
        runRows(workers, info.height, [&](int worker, uint32_t startRow, uint32_t endRow) {
            Histogram& local = localHistograms[worker];
            for (uint32_t y = startRow; y < endRow; ++y) {
                for (uint32_t x = 0; x < info.width; ++x) {
                    ++local[grayAt(view, x, y)];
                }
            }
        });
        countingNanos = nanosBetween(countingStart, Clock::now());
    }

    const auto mergeStart = Clock::now();
    Histogram merged{};
    merged.fill(0);
    for (const auto& local : localHistograms) {
        for (size_t bin = 0; bin < merged.size(); ++bin) {
            merged[bin] += local[bin];
        }
    }
    const int64_t mergeNanos = nanosBetween(mergeStart, Clock::now());

    AndroidBitmap_unlockPixels(env, bitmap);
    const int64_t totalNanos = nanosBetween(totalStart, Clock::now());

    jintArray countsArray = env->NewIntArray(static_cast<jsize>(merged.size()));
    if (countsArray == nullptr) {
        return nullptr;
    }
    env->SetIntArrayRegion(
        countsArray,
        0,
        static_cast<jsize>(merged.size()),
        reinterpret_cast<const jint*>(merged.data())
    );

    const jlong timings[6] = {
        nanosBetween(lockStart, lockEnd),
        grayscaleNanos,
        countingNanos,
        mergeNanos,
        totalNanos,
        workers,
    };
    env->SetLongArrayRegion(timingsArray, 0, 6, timings);
    return countsArray;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_lzx_imagehistogramanalyzer_data_image_NativeHistogramBridge_nativeGray(
    JNIEnv*,
    jobject,
    jint red,
    jint green,
    jint blue
) {
    return grayscale(
        static_cast<uint8_t>(std::clamp(red, 0, 255)),
        static_cast<uint8_t>(std::clamp(green, 0, 255)),
        static_cast<uint8_t>(std::clamp(blue, 0, 255))
    );
}
