package com.peregrine.sling.auth.header;

/*-
 * #%L
 * peregrinecms-com-peregrine-sling-auth-header
 * %%
 * Copyright (C) 2020 headwire inc.
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */

import javax.jcr.Credentials;
import java.util.Map;

/**
 *
 */
public class HeaderCredentials implements Credentials
{
    private String userId;
    private Map<String, Object> profile;

    public HeaderCredentials(String userId, Map<String, Object> profile)
    {
        this.userId = userId;
        this.profile = profile;
    }

    public String getUserId()
    {
        return userId;
    }

    public Map<String, Object> getProfile()
    {
        return profile;
    }
}
