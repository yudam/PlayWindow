package com.play.javalib;

import java.util.HashMap;

/**
 * User: maodayu
 * Date: 2023/12/8
 * Time: 16:49
 */
public class FindLenSubString {

    public static void main(String[] args) {

    }


    public static int findLongSubString(String s) {
        HashMap<Character, Integer> map = new HashMap<>();
        int maxLen = 0;
        int start = 0;
        int end = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (map.get(c) == null) {
                map.put(c, i);
                end = i;
            } else {

                int temp = end - start;
                if (temp > maxLen) {
                    maxLen = temp;
                }
                start = map.get(c) + 1;
            }
        }
        return maxLen;
    }
}
