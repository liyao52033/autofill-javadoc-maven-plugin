package com.liyao.autofillDoc;

import java.util.List;
import java.util.ArrayList;


public class TestService {


    public int add(int a, int b) {
        return a + b;
    }


    public String processUserData(String name, int age, List<String> hobbies) {
        StringBuilder sb = new StringBuilder();
        sb.append("姓名：").append(name).append("\n");
        sb.append("年龄：").append(age).append("\n");
        sb.append("爱好：").append(String.join(",", hobbies));
        return sb.toString();
    }


    public List<Integer> findPrimeNumbers(int start, int end) {
        List<Integer> primes = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            if (isPrime(i)) {
                primes.add(i);
            }
        }
        return primes;
    }


    private boolean isPrime(int num) {
        if (num < 2)
            return false;
        for (int i = 2; i <= Math.sqrt(num); i++) {
            if (num % i == 0)
                return false;
        }
        return true;
    }


    public void saveToFile(String filename, String content) throws Exception {
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        System.out.println("保存内容到文件：" + filename);
    }


    public User createUser(String username, String email, String password) {
        return new User(username, email, password);
    }

    static class User {

        private String username;

        private String email;

        private String password;

        public User(String username, String email, String password) {
            this.username = username;
            this.email = email;
            this.password = password;
        }
    }
}
