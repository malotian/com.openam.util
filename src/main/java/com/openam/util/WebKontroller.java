package com.openam.util;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebKontroller {

	@GetMapping("/contact")
	public String getContact(final Model model) {
		model.addAttribute("activePage", "contact");
		return "contact";
	}

	@GetMapping("/info")
	public String getInfo(final Model model) {
		model.addAttribute("activePage", "info");
		return "info";
	}
}