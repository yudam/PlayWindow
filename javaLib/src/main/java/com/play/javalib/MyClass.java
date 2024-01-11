package com.play.javalib;

import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class MyClass {


    public static LinkedList<Integer> taskList = new LinkedList<>();
    public static int maxTask = 5;
    public static Object lock = new Object();


    public static void main(String[] args) {

    }


    class Consumer{

        public void doWork() throws InterruptedException {
            while (true){
                synchronized (lock){
                    if(taskList.size() > 0 ){
                        taskList.remove();
                        lock.notifyAll();

                    } else {
                        lock.wait();
                    }
                    Thread.sleep(500);
                }
            }
        }
    }


    class Producer{
        public void doWork() throws InterruptedException {
            while (true){
                synchronized (lock){
                    if(taskList.size() < maxTask){
                        taskList.add(new Random().nextInt());
                        lock.notifyAll();
                    } else {
                        lock.wait();
                    }
                    Thread.sleep(500);
                }
            }
        }
    }
}