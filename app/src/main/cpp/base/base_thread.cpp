//
// Created by 毛大宇 on 2023/10/25.
//

#include <iostream>
#include <thread>
#include <mutex>

/**
 * 互斥锁，通过lock和unlock来实现手动加锁和解锁，类似于Java中的ReentrantLock
 */
std::mutex sync_mutex;

/**
 *  condition_variable的作用类似于Java中Object的wait和notify
 */
std::condition_variable cv;

void prepare() {


    /**
     * 作用和lock_guard类似，可以自动加锁和解锁
     */
    std::unique_lock<std::mutex> uniqueLock(sync_mutex);

    /**
     *  lock_guard的作用在于自动加锁和解锁，防止死锁的存在
     */
    std::lock_guard<std::mutex> lockGuard(sync_mutex);

    /**
     * 通过mutex来手动加锁和解锁
     */
    sync_mutex.lock();

    sync_mutex.unlock();

    cv.notify_all();
    cv.wait(uniqueLock);
}

int main() {

    std::thread t(prepare);
    t.join();
}