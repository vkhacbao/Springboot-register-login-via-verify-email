package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import com.example.ConfirmationToken;
import com.example.User;
import com.example.repository.ConfirmationTokenRepository;
import com.example.repository.UserRepository;
import com.example.service.EmailSenderService;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;





@Controller
public class UserAccountController {

	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ConfirmationTokenRepository confirmationTokenRepository;
	
	@Autowired
	private EmailSenderService emailSenderService;
	 
	
	
	// https://stackabuse.com/password-encoding-with-spring-security/
	// to encode our password
	BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

	// Registration
	@RequestMapping(value="/register", method=RequestMethod.GET)
	public ModelAndView displayRegistration(ModelAndView modelAndView, User user) {
		modelAndView.addObject("user", user);
		modelAndView.setViewName("register");
		return modelAndView;
	}
	
	@RequestMapping(value="/register", method=RequestMethod.POST)
	public ModelAndView registerUser(ModelAndView modelAndView, User user) {
		
		User existingUser = userRepository.findByEmailIdIgnoreCase(user.getEmailId());
		if(existingUser != null) {
			modelAndView.addObject("message","This email already exists!");
			modelAndView.setViewName("error");
		} else {
			user.setPassword(encoder.encode(user.getPassword()));
			userRepository.save(user);
			
			ConfirmationToken confirmationToken = new ConfirmationToken(user);
			
			confirmationTokenRepository.save(confirmationToken);
			
			SimpleMailMessage mailMessage = new SimpleMailMessage();
			mailMessage.setTo(user.getEmailId());
			mailMessage.setSubject("Complete Registration!");
			mailMessage.setFrom("nairobley@gmail.com");
			mailMessage.setText("To confirm your account, please click here : "
			+"http://localhost:8082/confirm-account?token="+confirmationToken.getConfirmationToken());
			
			emailSenderService.sendEmail(mailMessage);
			
			modelAndView.addObject("emailId", user.getEmailId());
			
			modelAndView.setViewName("successfulRegisteration");
		}
		
		return modelAndView;
	}

	// Confirm registration
	@RequestMapping(value="/confirm-account", method= {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView confirmUserAccount(ModelAndView modelAndView, @RequestParam("token")String confirmationToken) {
		ConfirmationToken token = confirmationTokenRepository.findByConfirmationToken(confirmationToken);
		
		if(token != null )
		{
			
			User user = userRepository.findByEmailIdIgnoreCase(token.getUser().getEmailId());
			user.setIsEnabled(1);
			userRepository.save(user);
			modelAndView.setViewName("accountVerified");
		}
		else
		{
			modelAndView.addObject("message","The link is invalid or broken!");
			modelAndView.setViewName("error");
		}
		
		return modelAndView;
	}	

	// Login
	@RequestMapping(value="/login", method=RequestMethod.GET)
	public ModelAndView displayLogin(ModelAndView modelAndView, User user) {
		modelAndView.addObject("user", user);
		modelAndView.setViewName("login");
		return modelAndView;
	}

	@RequestMapping(value="/login", method=RequestMethod.POST)
	public ModelAndView loginUser(ModelAndView modelAndView, User user) {
		
		User existingUser = userRepository.findByEmailIdIgnoreCase(user.getEmailId());
		/**
		 *  Check registered email or not
		 */
		if (existingUser == null) {
			modelAndView.addObject("message", "The email not registered");
			modelAndView.setViewName("login");
			
		}
		/**
		 * Check email registered ,verified and password invalid
		 */
		
		if (existingUser != null ) {
			if (existingUser.getIsEnabled()==0) {
		modelAndView.addObject("message", "The email not verified");
		modelAndView.setViewName("login");
	}
			
			else if (encoder.matches(user.getPassword(), existingUser.getPassword())){
				modelAndView.addObject("message", "Successfully logged in!");
				modelAndView.setViewName("successLogin");
				
			}
			else {
				modelAndView.addObject("message", "Incorrect password. Try again");
				modelAndView.setViewName("login");
			}
		}
			
		return modelAndView;
	}
	
	/**
	 * Display the forgot password page and form
	 */
	@RequestMapping(value="/forgot-password", method=RequestMethod.GET)
	public ModelAndView displayResetPassword(ModelAndView modelAndView, User user) {
		modelAndView.addObject("user", user);
		modelAndView.setViewName("forgotPassword");
		return modelAndView;
	}

	/**
	 * Receive email of the user, create token and send it via email to the user
	 */
	@RequestMapping(value="/forgot-password", method=RequestMethod.POST)
	public ModelAndView forgotUserPassword(ModelAndView modelAndView, User user) {
		User existingUser = userRepository.findByEmailIdIgnoreCase(user.getEmailId());
		if(existingUser != null) {
			// create token
			ConfirmationToken confirmationToken = new ConfirmationToken(existingUser);
			
			// save it
			confirmationTokenRepository.save(confirmationToken);
			
			// create the email
			SimpleMailMessage mailMessage = new SimpleMailMessage();
			mailMessage.setTo(existingUser.getEmailId());
			mailMessage.setSubject("Complete Password Reset!");
			mailMessage.setFrom("nairobley@gmail.com");
			mailMessage.setText("To complete the password reset process, please click here: "
			+"http://localhost:8082/confirm-reset?token="+confirmationToken.getConfirmationToken());
			
			emailSenderService.sendEmail(mailMessage);

			modelAndView.addObject("message", "Request to reset password received. Check your inbox for the reset link.");
			modelAndView.setViewName("successForgotPassword");

		} else {	
			modelAndView.addObject("message", "This email does not exist!");
			modelAndView.setViewName("error");
		}
		
		return modelAndView;
	}


	@RequestMapping(value="/confirm-reset", method= {RequestMethod.GET, RequestMethod.POST})
	public ModelAndView validateResetToken(ModelAndView modelAndView, @RequestParam("token")String confirmationToken)
	{
		ConfirmationToken token = confirmationTokenRepository.findByConfirmationToken(confirmationToken);
		
		if(token != null) {
			User user = userRepository.findByEmailIdIgnoreCase(token.getUser().getEmailId());
			user.setIsEnabled(1);
		
			userRepository.save(user);
			modelAndView.addObject("user", user);
			modelAndView.addObject("emailId", user.getEmailId());
			modelAndView.setViewName("resetPassword");
		} else {
			modelAndView.addObject("message", "The link is invalid or broken!");
			modelAndView.setViewName("error");
		}
		
		return modelAndView;
	}	

	/**
	 * Receive the token from the link sent via email and display form to reset password
	 */
	@RequestMapping(value = "/reset-password", method = RequestMethod.POST)
	public ModelAndView resetUserPassword(ModelAndView modelAndView, User user) {
		// ConfirmationToken token = confirmationTokenRepository.findByConfirmationToken(confirmationToken);
		
		if(user.getEmailId() != null) {
			// use email to find user
			User tokenUser = userRepository.findByEmailIdIgnoreCase(user.getEmailId());
			tokenUser.setIsEnabled(1);
			tokenUser.setPassword(encoder.encode(user.getPassword()));
			// System.out.println(tokenUser.getPassword());
			userRepository.save(tokenUser);
			userRepository.save(user);
			modelAndView.addObject("message", "Password successfully reset. You can now log in with the new credentials.");
			modelAndView.setViewName("successResetPassword");
		} else {
			modelAndView.addObject("message","The link is invalid or broken!");
			modelAndView.setViewName("error");
		}
	
		return modelAndView;
	}

@GetMapping("/test")
public ModelAndView test (ModelAndView modelAndView, User user) {
	User enabled = userRepository.getIsEnabled(user.getIsEnabled());
	if (enabled.getIsEnabled()==0) {
		modelAndView.setViewName("ok");
	}
	return modelAndView;
}
}