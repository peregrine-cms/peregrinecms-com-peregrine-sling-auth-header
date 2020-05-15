/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.peregrine.sling.auth.header;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;

@ObjectClassDefinition(name = "Header Authentication Handler Configuration", description = "Configuration options for the Header Authentication Handler.")
public @interface HeaderAuthenticationHandlerConfig {

	public static final String DEFAULT_HEADER_AUTH_LOGIN_COOKIE = "mod_auth_openidc_session";
	public static final String DEFAULT_HEADER_AUTH_REMOTE_USER_HEADER = "REMOTE_USER";

	@AttributeDefinition(name = "Login Cookie", description = "The name of the login cookie (if there is one) that indicates that a user is logged in.")
	String header_auth_login_cookie() default DEFAULT_HEADER_AUTH_LOGIN_COOKIE;

	@AttributeDefinition(name = "Remote User Header", description = "The HTTP header used to identify the logged in user")
	String header_auth_remote_user_header() default DEFAULT_HEADER_AUTH_REMOTE_USER_HEADER;
}
