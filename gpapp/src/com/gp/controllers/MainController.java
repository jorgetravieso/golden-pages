package com.gp.controllers;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.JSONObject;

import com.gp.users.UserDAO;
import com.gp.users.UserDTO;
import com.gp.users.UserDTO.ColumnName;
import com.gp.users.UserFactory;
import com.gp.util.Helper;

/**
 * Servlet implementation class MainController
 */
@WebServlet("/MainController")
public class MainController extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MainController() {
		super();
		// TODO Auto-generated constructor stub
	}

	
	public static final String REGISTER_ACTION = "REGISTER";
	public static final String LOGIN_ACTION = "LOGIN";
	public static final String LOGOUT_ACTION = "LOGOUT";
	
	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		String action = request.getParameter("action");
		String context = request.getServletContext().getContextPath();
		
		switch(action){
		case  LOGOUT_ACTION:
			logout(request);
			response.sendRedirect("/gpapp");
			//getServletContext().getRequestDispatcher("/").forward(request, response); 
			break;
		default:
			System.out.println("GET request received without any action");
			break;
		}
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {

		JSONObject responseJSON = null; 
		String action = request.getParameter("action");


		switch (action) {
		case REGISTER_ACTION:
			responseJSON = register(request);
			break; 
		case LOGIN_ACTION:
			responseJSON = login(request);
			break;

		default:
			responseJSON = new JSONObject();
			responseJSON.put("status", "Incomplete");
			break;
		}

		PrintWriter out = response.getWriter();
		response.setContentType("application/json");
		out.println(responseJSON);
		out.flush();

	}
	
	
	public JSONObject register(HttpServletRequest request){
		JSONObject responseJSON = new JSONObject();
		if (request.getSession().getAttribute("user") != null) {
		//	System.out.println("User already exist");
			responseJSON.put("status", "ERROR");
			responseJSON.put("message",
					"Trying to create a duplicate account");
			return responseJSON;
		}
		UserFactory factory = new UserFactory("jdbc/gpappdb"); 
		UserDTO user = new UserDTO();
		String option = request.getParameter("options");
		user.setFullName(request.getParameter("full-name"));
		user.setUsername(request.getParameter("username"));
		user.setEmail(request.getParameter("email"));
		user.setPassword(request.getParameter("password"));
		String passConfirm = request.getParameter("password-confirm");
		if (!Helper.isValid(option, user.getEmail(), user.getFullName(),
				user.getPassword(), user.getUsername(), passConfirm)) {
			System.out.println("ERROR IN INPUT");
			responseJSON.put("status", "ERROR");
			responseJSON.put("message", "Missing attribute");
			return responseJSON;
		}
		if (!user.getPassword().equals(passConfirm)) {
			responseJSON.put("status", "ERROR");
			responseJSON.put("message", "The passwords didn't match.");
			return responseJSON;
		}
		String rawPass = user.getPassword();
		user.setPassword(DigestUtils.md5Hex(rawPass));
		try {
			UserDAO userDAO = new UserDAO("jdbc/gpappdb");
			if (userDAO.existAs(user.getUsername(), ColumnName.USERNAME)
					|| userDAO.existAs(user.getEmail(), ColumnName.EMAIL)) {
				responseJSON.put("status", "ERROR");
				responseJSON.put("message",
						"The username or email is already taken");
				return responseJSON;
			}

			System.out.println("option: " + option);
			if (option.equals("opt-biz")) {
				String bizName = request.getParameter("bname");
				factory.createBusinessUser(bizName, user);
			} else if (option.equals("opt-cons")) {
				factory.createConsumerUser(user);
			}
		} catch (Exception e) {
			e.printStackTrace();
			responseJSON.put("status", "ERROR");
			responseJSON.put("message", "SQL problem");
			return responseJSON;
		}
		responseJSON.put("message", "The account was created");
		responseJSON.put("action", "reload");
		request.getSession().setAttribute("user", user);
		return responseJSON;
	}

	public JSONObject login(HttpServletRequest request){
		JSONObject response = new JSONObject();
		
		UserDTO user = (UserDTO) request.getSession().getAttribute("user");
		if (user != null){
			response.put("status", "ERROR");
			response.put("message",
					"Trying to create a duplicate account");
			return response;
		}
		UserDAO dao = new UserDAO("jdbc/gpappdb");
		String username = request.getParameter("log_username");
		String rawPassword = request.getParameter("log_password");
		if(!Helper.isValid(username, rawPassword)){
			response.put("status", "ERROR");
			response.put("message",
					"Missing attributes");
			return response;
		}
		String password = DigestUtils.md5Hex(rawPassword);
		String id = dao.verifyUserAndGetID(username, password);
		if(id == null){
			response.put("status", "ERROR");
			response.put("message",
					"The provided credentials are invalid, user not found");
			return response;
		}
		user = dao.find(id);
		response.put("status", "OK");
		response.put("message", "Successful login");
		response.put("action", "reload");
		request.getSession().setAttribute("user", user);
		return response;
	}
	
	public JSONObject logout(HttpServletRequest request){
		JSONObject response = new JSONObject();
		if (request.getSession().getAttribute("user") == null) {
			//	System.out.println("User already exist");
			response.put("status", "ERROR");
			response.put("message",
						"Trying to logout when the user is not logged in");
				return response;
		}
		
		
		response.put("message", "Successful logout");
		response.put("action", "reload");
		request.getSession().removeAttribute("user");
		return response;
	}
}
