package pl.learnedge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class UserListController {

    @GetMapping("/lista-uzytkowników")
    public String userList() {
        return "dashboard/user-list";
    }
}
