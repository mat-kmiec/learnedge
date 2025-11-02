package pl.learnedge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pl.learnedge.dto.LessonDto;
import pl.learnedge.exception.LessonNotFoundException;
import pl.learnedge.model.Lesson;
import pl.learnedge.model.LessonProgress;
import pl.learnedge.model.User;
import pl.learnedge.repository.LessonProgressRepository;
import pl.learnedge.repository.LessonRepository;
import pl.learnedge.repository.UserRepository;
import pl.learnedge.service.AuthService;
import pl.learnedge.service.LessonService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class LessonController {

    private final LessonService lessonService;
    private final AuthService authService;
    private final LessonRepository lessonRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRepository userRepository;

    @GetMapping("/kurs/{course_slug}/{lesson_slug}")
    public String lesson(@PathVariable String course_slug, @PathVariable String lesson_slug, Model model) {
        LessonDto lesson = lessonService.getLessonBySlug(lesson_slug);
        model.addAttribute("lesson", lesson);
        return "course/lesson";
    }

    @GetMapping("/kreator-lekcji/{course_id}/{course_name}")
    public String lessonCreator(@PathVariable("course_id") Long courseId,
                                @PathVariable("course_name") String courseName,
                                Model model) {
        model.addAttribute("courseId", courseId);
        model.addAttribute("courseName", courseName);
        return "course/create-lesson";
    }

    @PostMapping(
            value = "/api/lessons/save",
            consumes = {"multipart/form-data"}
    )
    @ResponseBody
    public ResponseEntity<?> saveLesson(
            @RequestParam("courseId") Long courseId,
            @RequestParam("title") String title,
            @RequestParam("contentHtml") String contentHtml,
            @RequestParam(value = "images", required = false) List<MultipartFile> images,
            @RequestParam(value = "imageNames", required = false) List<String> imageNames,
            @RequestParam(value = "audio", required = false) List<MultipartFile> audio,
            @RequestParam(value = "audioNames", required = false) List<String> audioNames
    ) {

        lessonService.saveLesson(courseId, title, contentHtml, images, imageNames, audio, audioNames);
        return ResponseEntity.ok("Lesson saved successfully");
    }

    @PutMapping("/api/{lessonId}/complete")
    public ResponseEntity<?> completeLesson(@PathVariable Long lessonId) {
        Long userId = authService.getCurrentUserId();
        lessonService.markLessonAsCompleted(lessonId, userId);
        return null;
    }


}


