/* Copyright 2004, 2005, 2006 Acegi Technology Pty Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.security.web.access.intercept;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.*;
import javax.servlet.FilterChain;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDecisionManager;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.access.SecurityConfig;
import org.springframework.security.access.event.AuthorizedEvent;
import org.springframework.security.access.intercept.RunAsManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.FilterInvocation;


/**
 * Tests {@link FilterSecurityInterceptor}.
 *
 * @author Ben Alex
 * @author Luke Taylor
 */
public class FilterSecurityInterceptorTests {
    private AuthenticationManager am;
    private AccessDecisionManager adm;
    private FilterInvocationSecurityMetadataSource ods;
    private RunAsManager ram;
    private FilterSecurityInterceptor interceptor;
    private ApplicationEventPublisher publisher;


    //~ Methods ========================================================================================================

    @Before
    public final void setUp() throws Exception {
        interceptor = new FilterSecurityInterceptor();
        am = mock(AuthenticationManager.class);
        ods = mock(FilterInvocationSecurityMetadataSource.class);
        adm = mock(AccessDecisionManager.class);
        ram = mock(RunAsManager.class);
        publisher = mock(ApplicationEventPublisher.class);
        interceptor.setAuthenticationManager(am);
        interceptor.setSecurityMetadataSource(ods);
        interceptor.setAccessDecisionManager(adm);
        interceptor.setRunAsManager(ram);
        interceptor.setApplicationEventPublisher(publisher);
        SecurityContextHolder.clearContext();
    }

    @After
    public void tearDown() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEnsuresAccessDecisionManagerSupportsFilterInvocationClass() throws Exception {
        when(adm.supports(FilterInvocation.class)).thenReturn(true);
        interceptor.afterPropertiesSet();
    }

    @Test(expected=IllegalArgumentException.class)
    public void testEnsuresRunAsManagerSupportsFilterInvocationClass() throws Exception {
        when(adm.supports(FilterInvocation.class)).thenReturn(false);
        interceptor.afterPropertiesSet();
    }

    /**
     * We just test invocation works in a success event. There is no need to test access denied events as the
     * abstract parent enforces that logic, which is extensively tested separately.
     */
    @Test
    public void testSuccessfulInvocation() throws Throwable {
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath("/secure/page.html");

        // Setup a Context
        final Authentication token = new TestingAuthenticationToken("Test", "Password", "NOT_USED");
        SecurityContextHolder.getContext().setAuthentication(token);

        // Create and test our secure object
        final FilterChain chain = mock(FilterChain.class);
        final FilterInvocation fi = new FilterInvocation(request, response, chain);
        final List<ConfigAttribute> attributes = SecurityConfig.createList("MOCK_OK");

        when(ods.getAttributes(fi)).thenReturn(attributes);

        interceptor.invoke(fi);

        verify(publisher).publishEvent(any(AuthorizedEvent.class));        
    }
}
