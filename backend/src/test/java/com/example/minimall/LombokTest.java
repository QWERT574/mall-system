package com.example.minimall;

import lombok.Data;

@Data
public class LombokTest {
    private String name;
    private int age;
    
    public static void main(String[] args) {
        LombokTest test = new LombokTest();
        test.setName("Test");
        test.setAge(20);
        System.out.println("Name: " + test.getName());
        System.out.println("Age: " + test.getAge());
        System.out.println("Lombok works!");
    }
}