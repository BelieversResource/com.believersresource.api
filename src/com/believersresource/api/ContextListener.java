package com.believersresource.api;

import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Enumeration;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

public class ContextListener implements ServletContextListener {

	private ServletContext context = null;

	  /*This method is invoked when the Web Application has been removed  and is no longer able to accept requests */

	  public void contextDestroyed(ServletContextEvent event)
	  {
		  	Enumeration<Driver> drivers = DriverManager.getDrivers();
			while (drivers.hasMoreElements()) {
			    Driver driver = drivers.nextElement();
			    try {
			        DriverManager.deregisterDriver(driver);
			        //LOG.log(Level.INFO, String.format("deregistering jdbc driver: %s", driver));
			    } catch (SQLException e) {
			        //LOG.log(Level.SEVERE, String.format("Error deregistering driver %s", driver), e);
			    }
			}
			this.context = null;
	  }


	  //This method is invoked when the Web Application is ready to service requests
	  public void contextInitialized(ServletContextEvent event)
	  {
	    this.context = event.getServletContext();
	  }
}
