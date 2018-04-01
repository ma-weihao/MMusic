package club.wello.mmusic.util;

import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Created by maweihao on 23/01/2018.
 */

public class Shuffle {

    public static int next(int totalNum, int playing, int last) {
        if (playing >= totalNum || last >= totalNum) {
            throw new RuntimeException("parameter error! " + totalNum + " " + playing + " " + last);
        }
        if (totalNum > 2) {
            Random random = new Random();
            int next;
            do {
                next = random.nextInt(totalNum);
            } while (next == playing || next == last);
            return next;
        } else if (totalNum == 2) {
            return playing == 0 ? 1 : 0;
        } else if (totalNum == 1) {
            return playing;
        } else {
            throw new RuntimeException("num should greater than 0");
        }
    }

    public static int next(int totalNum, int playing) {
        if (playing >= totalNum) {
            throw new RuntimeException("parameter error! " + totalNum + " " + playing);
        }
        if (totalNum > 1) {
            Random random = new Random();
            int next;
            do {
                next = random.nextInt(totalNum);
            } while (next == playing);
            return next;
        } else if (totalNum == 1) {
            return playing;
        } else {
            throw new RuntimeException("num should greater than 0");
        }
    }

    /**
     * return an shuffled array in the given length
     * @param length length of array
     * @return shuffled array
     */
    public static int[] shuffleList(int length) {
        if (length <= 0) {
            throw new RuntimeException("array length should be positive");
        }
        int[] array = new int[length];
        for (int i = 0; i < length; i++) {
            array[length - i - 1] = i;
        }
        Shuffle.shuffleList(array);
        return array;
    }

    /**
     * shuffle the given array
     * @param array original array
     */
    public static void shuffleList(int[] array) {
        if (array == null || array.length == 0) {
            throw new RuntimeException("array can't be null");
        }
        if (array.length == 1) {
            return;
        }
        Random random = new Random();
        int length = array.length;
        int index;
        int value;
        int median;
        for(index = 0; index < length; index ++){
            value = index + random.nextInt(length - index);

            median = array[index];
            array[index] = array[value];
            array[value] = median;
        }
    }

    public static void shuffleList(List list) {
        Collections.shuffle(list);
    }
}
