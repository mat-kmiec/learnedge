package pl.learnedge.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import pl.learnedge.dto.CourseDto;
import pl.learnedge.service.CourseService;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class AdminController {

    private final CourseService courseService;

    @GetMapping("/admin")
    public String admin(Model model) {
        List<CourseDto> courses = courseService.getAllCourse();
        model.addAttribute("courses", courses);
        model.addAttribute("newCourse", new CourseDto());

        return "dashboard/admin";
    }

    @PostMapping("/admin")
    public String createCourse(@ModelAttribute CourseDto newCourseData){
        courseService.createCourse(newCourseData);
        return "redirect:/admin";
    }

}
