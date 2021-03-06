package com.Controllers;



import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Entities.ERole;
import com.Entities.Role;
import com.Entities.User;
import com.Repository.RoleRepository;
import com.Repository.UserRepository;
import com.Security.jwt.JwtUtils;
import com.Security.services.UserDetailsImp;
import com.actions.request.LoginRequest;
import com.actions.request.SignupRequest;
import com.actions.response.JwtResponse;
import com.actions.response.MessageResponse;

@RestController
@CrossOrigin(origins = "*", maxAge = 3600)
@RequestMapping("/api/auth")
public class AuthController {
	
	
	@Autowired
	AuthenticationManager authenticationManager;
	
	@Autowired
	UserRepository userRepository;
	
	@Autowired
	RoleRepository roleRepository;
	
	@Autowired
	PasswordEncoder encoder;
	
	@Autowired
	JwtUtils jwtUtils;
	
	@PostMapping("/signin")
	public ResponseEntity<?> authenticationUser(@Valid @RequestBody LoginRequest loginRequest){
		Authentication authentication = authenticationManager.authenticate(
				new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));
		
		SecurityContextHolder.getContext().setAuthentication(authentication);
		String jwt = jwtUtils.generateJwtToken(authentication);
		
		UserDetailsImp userDetails = (UserDetailsImp) authentication.getPrincipal();
		List<String> roles = userDetails.getAuthorities().stream()
				.map(item -> item.getAuthority())
				.collect(Collectors.toList());
		
		return ResponseEntity.ok(new JwtResponse(jwt,
				userDetails.getId(),
				userDetails.getUsername(),
				userDetails.getEmail(),
				roles));
	}
	
	@PostMapping("/signup")
	public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest singnupRequest){
		if (userRepository.existsByUsername(singnupRequest.getUsername())) {
			return ResponseEntity.badRequest()
					.body(new MessageResponse("Error: Username is already taken!"));
		}
		
		if (userRepository.existsByEmail(singnupRequest.getEmail())) {
			return ResponseEntity
					.badRequest()
					.body(new MessageResponse("Error: Email is already taken!"));
		}
		
		//create new user's account
		User user = new User(singnupRequest.getUsername(), 
				singnupRequest.getEmail(), encoder.encode(singnupRequest.getPassword()));
		
		Set<String> strRoles = singnupRequest.getRole();
		Set<Role> roles = new HashSet<>();
		
		if (strRoles == null ) {
			Role userRole = roleRepository.findByName(ERole.ROLE_CITIZEN)
					.orElseThrow(() -> new RuntimeException("Error: Role is not found"));
			roles.add(userRole);
		}else {
			strRoles.forEach(role -> {
				switch (role) {
				case "administrator":
					Role adminRole = roleRepository.findByName(ERole.ROLE_ADMINISTRATOR)
										.orElseThrow(() -> new RuntimeException("Error: Role is not found"));
					roles.add(adminRole);
					break;
				
				case "agent" :
					Role modRole = roleRepository.findByName(ERole.ROLE_AGENT)
										.orElseThrow(() -> new RuntimeException("Error: Role is not found"));
					roles.add(modRole);
					break;					
				default:
					Role userRole = roleRepository.findByName(ERole.ROLE_CITIZEN)
										.orElseThrow(() -> new RuntimeException("Error: Role is not found"));
					roles.add(userRole);
				}
			});
		}
		
		user.setRoles(roles);
		userRepository.save(user);
		
		
		return ResponseEntity.ok(new MessageResponse("User registered successfully"));
		
		
	}
	
	
}
