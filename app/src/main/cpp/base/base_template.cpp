//
// Created by 毛大宇 on 2023/10/25.
//

#include <iostream>


/**
 *  typedef的作用就是给类型起一个别名
 */
typedef int my_int;

/**
 * template 定义模版类
 *
 * typename 类型说明符，也可以用class来表示
 */
template<typename T>
void template1_function(T t) {

}

template<class D>
void template2_function(D &d) {

}