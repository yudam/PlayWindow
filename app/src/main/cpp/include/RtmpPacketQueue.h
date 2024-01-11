//
// Created by 毛大宇 on 2023/7/17.
//

#ifndef PLAYWINDOW_RTMPPACKETQUEUE_H
#define PLAYWINDOW_RTMPPACKETQUEUE_H

#include <queue>
#include <thread>


template<class T>
class RtmpPacketQueue {

private:

    std::mutex m_mutex;
    std::queue<T> m_queue;

public:

    void push(T t_value) {
        std::lock_guard<std::mutex> lock(m_mutex);
        m_queue.push(t_value);
    }

    int pop(T &t_value) {
        std::lock_guard<std::mutex> lock(m_mutex);

        int ret = 0;
        if(!m_queue.empty()){
            // front 表示返回第一个元素
            t_value = m_queue.front();
            // pop 表示删除第一个元素
            m_queue.pop();
            ret = 1;
        }
        return ret;
    }


    void clear() {
        std::lock_guard<std::mutex> lock(m_mutex);
    }


    bool empty() {
        std::lock_guard<std::mutex> lock(m_mutex);
        return m_queue.empty();
    }

    int size() {
        std::lock_guard<std::mutex> lock(m_mutex);
        return m_queue.size();
    }

};

#endif //PLAYWINDOW_RTMPPACKETQUEUE_H
