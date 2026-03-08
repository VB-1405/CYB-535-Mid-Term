package com.vrishabh.midterm.cicd;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MathUtilsTest {
    MathUtils mathUtils = new MathUtils();

    @Test
    public void testAdd() {
        assertEquals(3, mathUtils.add(1, 2));
        assertEquals(0, mathUtils.add(-1, 1));
        assertEquals(0, mathUtils.add(0, 0));
        assertEquals(-8, mathUtils.add(-5, -3));
    }
    
    @Test
    public void testSubtract() {
        assertEquals(3, mathUtils.subtract(5, 2));
        assertEquals(-3, mathUtils.subtract(2, 5));
        assertEquals(0, mathUtils.subtract(0, 0));
        assertEquals(-2, mathUtils.subtract(-5, -3));
    }
    
    @Test
    public void testMultiply() {
        assertEquals(6, mathUtils.multiply(2, 3));
        assertEquals(-6, mathUtils.multiply(-2, 3));
        assertEquals(0, mathUtils.multiply(0, 5));
        assertEquals(8, mathUtils.multiply(-4, -2));
    }
    
    @Test
    public void testDivide() {
        assertEquals(5.0, mathUtils.divide(10, 2), 0.001);
        assertEquals(3.5, mathUtils.divide(7, 2), 0.001);
        assertEquals(-1.0, mathUtils.divide(1, 0), 0.001);
        assertEquals(-5.0, mathUtils.divide(-10, 2), 0.001);
        assertEquals(0.0, mathUtils.divide(0, 5), 0.001);
    }
}
