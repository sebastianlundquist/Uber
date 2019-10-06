package com.sebastianlundquist.uber;

import com.parse.Parse;
import com.parse.ParseACL;

import android.app.Application;

public class StarterApp extends Application {
	@Override
	public void onCreate() {
		super.onCreate();
		Parse.enableLocalDatastore(this);
		Parse.initialize(new Parse.Configuration.Builder(this)
				.applicationId("2b93ad3b1c4ff1b9a5d1182c97b3e7cde104e0ec")
				.clientKey("71a08cc271e3ce7b24b30802352bf27d39e15b91")
				.server("http://13.53.200.191:80/parse/")
				.build()
		);
		ParseACL defaultACL = new ParseACL();
		defaultACL.setPublicReadAccess(true);
		defaultACL.setPublicWriteAccess(true);
		ParseACL.setDefaultACL(defaultACL, true);
	}
}
