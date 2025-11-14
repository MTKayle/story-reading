package org.example.storyreading.storyservice.util;

public class SlugUtil {
    public static String slugify(String input) {
        if (input == null) return "";
        String s = input.toLowerCase()
                .replaceAll("[^a-z0-9\s-]", "")
                .replaceAll("[\s-]+", "-")
                .replaceAll("^-+|-+$", "");
        return s;
    }
}


