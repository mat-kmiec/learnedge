package pl.learnedge.controller;

import lombok.AllArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import pl.learnedge.dto.CourseDto;
import pl.learnedge.exception.UserAlreadyEnrollException;
import pl.learnedge.model.Course;
import pl.learnedge.model.User;
import pl.learnedge.service.AuthService;
import pl.learnedge.service.CourseService;

import java.util.List;
import java.util.function.Function;

@Controller
@AllArgsConstructor
public class CourseController {

    private final CourseService courseService;
    private final AuthService authService;

    @GetMapping("/dostepne-kursy")
    public String course(Model model) {
        return loadCourses(model, courseService::getAvailableCoursesForUser, "dashboard/available-courses");
    }

    @GetMapping("/panel")
    public String panel(Model model) {
        return loadCourses(model, courseService::getEnrolledCoursesForUser, "dashboard/dashboard");
    }

    private String loadCourses(Model model,
                               Function<Long, List<CourseDto>> serviceMethod,
                               String viewName) {
        long userId = authService.getCurrentUserId();
        List<CourseDto> courses = serviceMethod.apply(userId);
        model.addAttribute("courses", courses);
        return viewName;
    }

    @GetMapping("/kurs/{slug}")
    public String course(@PathVariable String slug, Model model){
        long userId = authService.getCurrentUserId();
        CourseDto course =  courseService.getCourseBySlug(slug, userId);
        model.addAttribute("course", course);
        return "course/course";
    }

    @GetMapping("/zapisz-na-kurs/{id}")
    public String enrollToCourse(@PathVariable Long id, RedirectAttributes redirectAttributes){
        Long userId = authService.getCurrentUserId();
        try{
            courseService.enrollUserForCourse(id, userId);
            redirectAttributes.addFlashAttribute("successMessage", "Zostałeś zapisany na kurs!");
        }catch (UserAlreadyEnrollException e){
            redirectAttributes.addFlashAttribute("errorMessage", "Jesteś już zapisany na ten kurs!");
        }
        return "redirect:/panel";
    }


}
