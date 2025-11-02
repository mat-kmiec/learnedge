package pl.learnedge.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import pl.learnedge.dto.LessonDto;
import pl.learnedge.exception.LessonNotFoundException;
import pl.learnedge.exception.UserNotFoundException;
import pl.learnedge.mapper.LessonMapper;
import pl.learnedge.model.Course;
import pl.learnedge.model.Lesson;
import pl.learnedge.model.LessonProgress;
import pl.learnedge.model.User;
import pl.learnedge.repository.CourseRepository;
import pl.learnedge.repository.LessonProgressRepository;
import pl.learnedge.repository.LessonRepository;
import pl.learnedge.repository.UserRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LessonService {

    private final LessonRepository lessonRepository;
    private final LessonMapper lessonMapper;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final SlugService slugService;

    public LessonDto getLessonBySlug(String slug) {
        Lesson lesson = lessonRepository.findBySlug(slug)
                .orElseThrow(LessonNotFoundException::new);
        return lessonMapper.toDto(lesson);
    }

    public void saveLesson(Long courseId,
                           String title,
                           String contentHtml,
                           List<MultipartFile> images,
                           List<String> imageNames,
                           List<MultipartFile> audioFiles,
                           List<String> audioNames) {

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("Nie znaleziono kursu o ID: " + courseId));

        Lesson lesson = new Lesson();
        lesson.setTitle(title);
        lesson.setCourse(course);
        int nextOrder = getLastLessonOrder(courseId) + 1;
        lesson.setLessonOrder(nextOrder);
        lesson.setSlug(slugService.generateSlug(title));
        lesson.setContent("");
        lesson = lessonRepository.save(lesson);

        Path baseDir = Paths.get("uploads", "courses", course.getSlug(), lesson.getId().toString());
        try {
            Files.createDirectories(baseDir);
        } catch (IOException e) {
            throw new RuntimeException("Nie można utworzyć katalogu lekcji: " + baseDir, e);
        }

        Map<String, String> imageReplacements = new HashMap<>();
        Map<String, String> audioReplacements = new HashMap<>();

        saveUploadedFiles(images, imageNames, baseDir, imageReplacements);
        saveUploadedFiles(audioFiles, audioNames, baseDir, audioReplacements);

        String basePath = "/uploads/courses/" + course.getSlug() + "/" + lesson.getId() + "/";

        for (String oldName : imageReplacements.keySet()) {
            if (!oldName.toLowerCase().matches(".*\\.(jpg|jpeg|png)$")) continue;
            String escaped = Pattern.quote(oldName);
            contentHtml = contentHtml.replaceAll(
                    "(?i)(src=\")([^\"]*" + escaped + ")(\")",
                    "$1" + basePath + oldName + "$3"
            );
        }

        for (String oldName : audioReplacements.keySet()) {
            if (!oldName.toLowerCase().endsWith(".mp3")) continue;
            String escaped = Pattern.quote(oldName);
            contentHtml = contentHtml.replaceAll(
                    "(?i)(src=\")([^\"]*" + escaped + ")(\")",
                    "$1" + basePath + oldName + "$3"
            );
        }

        contentHtml = contentHtml.replaceAll("(?i)src=\"blob:[^\"]+\"", "");
        lesson.setContent(contentHtml);
        lessonRepository.save(lesson);

    }

    private void saveUploadedFiles(List<MultipartFile> files,
                                   List<String> namesFromJs,
                                   Path baseDir,
                                   Map<String, String> replacements) {

        if (files == null || files.isEmpty()) return;

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            if (file == null || file.isEmpty()) continue;

            String finalName;
            if (namesFromJs != null && i < namesFromJs.size() && namesFromJs.get(i) != null) {
                finalName = Paths.get(namesFromJs.get(i)).getFileName().toString();
            } else {
                finalName = UUID.randomUUID() + "-" + (file.getOriginalFilename() != null ? file.getOriginalFilename() : "plik");
            }

            Path destPath = baseDir.resolve(finalName);

            try {
                file.transferTo(destPath);
                replacements.put(finalName, finalName);
            } catch (IOException e) {
                throw new RuntimeException("Błąd przy zapisie pliku: " + finalName, e);
            }
        }
    }

//    private String generateSlug(String title) {
//        if (title == null) return "";
//        return title.toLowerCase()
//                .replaceAll("ą", "a").replaceAll("ć", "c").replaceAll("ę", "e")
//                .replaceAll("ł", "l").replaceAll("ń", "n").replaceAll("ó", "o")
//                .replaceAll("ś", "s").replaceAll("ź", "z").replaceAll("ż", "z")
//                .replaceAll("[^a-z0-9\\s-]", "")
//                .replaceAll("\\s+", "-")
//                .replaceAll("-{2,}", "-");
//    }

    private int getLastLessonOrder(Long courseId){
        return lessonRepository.findTopByCourseIdOrderByLessonOrderDesc(courseId)
                .map(Lesson::getLessonOrder)
                .orElse(0);
    }

    @Transactional
    public void markLessonAsCompleted(Long lessonId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(LessonNotFoundException::new);

        LessonProgress progress = lessonProgressRepository.findByLessonAndUser(lesson, user)
                .orElse(new LessonProgress());

        if (progress.isCompleted()) return;

        progress.setCompleted(true);
        progress.setUser(user);
        progress.setLesson(lesson);

        lessonProgressRepository.save(progress);

    }
}

