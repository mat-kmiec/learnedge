package pl.learnedge.service;

import org.springframework.stereotype.Service;

@Service
public class SlugService {
    public String generateSlug(String title) {
        if (title == null) return "";
        return title.toLowerCase()
                .replaceAll("ą", "a").replaceAll("ć", "c").replaceAll("ę", "e")
                .replaceAll("ł", "l").replaceAll("ń", "n").replaceAll("ó", "o")
                .replaceAll("ś", "s").replaceAll("ź", "z").replaceAll("ż", "z")
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-{2,}", "-");
    }
}
