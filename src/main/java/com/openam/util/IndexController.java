package com.openam.util;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class IndexController {

	@GetMapping("/info")
	public String getInfo(Model model) {
		model.addAttribute("activePage", "info");
		return "info";
	}

	@GetMapping("/contact")
	public String getContact(Model model) {
		model.addAttribute("activePage", "contact");
		return "contact";
	}
}