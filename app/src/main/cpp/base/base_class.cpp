//
// Created by 毛大宇 on 2023/10/30.
//

#include <iostream>


class ATest{


public:

    ATest():a(10),b(100){};

    ATest(const std::string username,int age);

    ~ATest();

private:

    int a;
    int b;

};


