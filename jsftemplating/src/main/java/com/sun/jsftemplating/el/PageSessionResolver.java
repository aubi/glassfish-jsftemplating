/*
 * Copyright (c) 2006, 2022 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */

package com.sun.jsftemplating.el;

import java.beans.FeatureDescriptor;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotWritableException;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.FacesContext;

/**
 * <p>
 * This <code>ELResolver</code> exists to resolve "page session" attributes. This concept, borrowed from
 * NetDynamics / JATO, stores data w/ the page so that it is available throughout the life of the page. This is longer
 * than request scope, but usually shorter than session. This implementation stores the attributes on the
 * <code>UIViewRoot</code>.
 * </p>
 *
 * @author Ken Paulsen (ken.paulsen@sun.com)
 */
public class PageSessionResolver extends ELResolver {

    /**
     * <p>
     * The name an expression must use when it explicitly specifies page session. ("pageSession")
     * </p>
     */
    public static final String PAGE_SESSION = "pageSession";

    /**
     * <p>
     * The attribute key in which to store the "page" session Map.
     * </p>
     */
    private static final String PAGE_SESSION_KEY = "_ps";

    /**
     * <p>
     * This first delegates to the original <code>ELResolver</code>, it then checks "page session" to see if the value
     * exists.
     * </p>
     */
    @Override
    public Object getValue(ELContext context, Object base, Object property) {
        Object value = null; // value to return
        if (base == null && PAGE_SESSION.equals(property)) {
            // the request is for the page session itself
            context.setPropertyResolved(true);
            //value = evaluatePageSessionMap(context);
            value = new VariableResolver.PageSessionDataSource();
        } else if (base != null && base instanceof VariableResolver.PageSessionDataSource) {
            // return the property from the provided page context
            VariableResolver.PageSessionDataSource psds = (VariableResolver.PageSessionDataSource)base;
            FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
            UIViewRoot root = facesContext.getViewRoot();
            value = psds.getValue(facesContext, null, root, property.toString());
            context.setPropertyResolved(true);
        } else if (base == null) {
            // try to find the property in the page context
            Map<String, Serializable> map = evaluatePageSessionMap(context);
            value = map.get(property.toString());
            if (value != null) {
                context.setPropertyResolved(true);
            }
        }
        return value;
    }

    private Map<String, Serializable> evaluatePageSessionMap(ELContext context) {
        FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
        UIViewRoot root = facesContext.getViewRoot();
        Map<String, Serializable> map = getPageSession(facesContext, root);
        if (map == null) {
            map = createPageSession(facesContext, root);
        }
        return map;
    }

    /**
     * <p>
     * This method provides access to the "page session" <code>Map</code>. If it doesn't exist, it returns
     * <code>null</code>. If the given <code>UIViewRoot</code> is null, then the current <code>UIViewRoot</code> will be
     * used.
     * </p>
     */
    public static Map<String, Serializable> getPageSession(FacesContext ctx, UIViewRoot root) {
        if (root == null) {
            root = ctx.getViewRoot();
        }
        return (Map<String, Serializable>) root.getAttributes().get(PAGE_SESSION_KEY);
    }

    /**
     * <p>
     * This method will create a new "page session" <code>Map</code>. It will overwrite any existing "page session"
     * <code>Map</code>, so be careful.
     * </p>
     */
    public static Map<String, Serializable> createPageSession(FacesContext ctx, UIViewRoot root) {
        if (root == null) {
            root = ctx.getViewRoot();
        }
        // Create it...
        Map<String, Serializable> map = new HashMap<>(4);

        // Store it...
        root.getAttributes().put(PAGE_SESSION_KEY, map);

        // Return it...
        return map;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        if (base == null && PAGE_SESSION.equals(property)) {
            // the request is for the page session itself
            throw new PropertyNotWritableException("PageSessionResolver doesn't support setValue. "
                    + "base=" + base + "property=" + property);
        } else if (base != null && base instanceof VariableResolver.PageSessionDataSource) {
            // set the property from the provided page context
            Map<String, Serializable> map = evaluatePageSessionMap(context);
            map.put(property.toString(), (Serializable) value);
            context.setPropertyResolved(true);
        } else if (base == null) {
            // try to find the property in the page context
            Map<String, Serializable> map = evaluatePageSessionMap(context);
            if (map.containsKey(property.toString())) {
                map.put(property.toString(), (Serializable) value);
                context.setPropertyResolved(true);
            }
        }
    }

    @Override
    public Class<?> getType(ELContext context, Object base, Object property) {
        Object value = null; // value to return
        Class<?> type = null;
        if (base == null && PAGE_SESSION.equals(property)) {
            // the request is for the page session itself
            context.setPropertyResolved(true);
            //value = evaluatePageSessionMap(context);
            type = VariableResolver.PageSessionDataSource.class;
//        } else if (base != null && base instanceof Map) {
        } else if (base != null && base instanceof VariableResolver.PageSessionDataSource) {
            // return the property from the provided page context
            VariableResolver.PageSessionDataSource psds = (VariableResolver.PageSessionDataSource) base;
            FacesContext facesContext = (FacesContext) context.getContext(FacesContext.class);
            UIViewRoot root = facesContext.getViewRoot();
            value = psds.getValue(facesContext, null, root, property.toString());
            context.setPropertyResolved(true);
            if (value != null) {
                type = value.getClass();
            }
        } else if (base == null) {
            // try to find the property in the page context
            Map<String, Serializable> map = evaluatePageSessionMap(context);
            value = map.get(property.toString());
            if (value != null) {
                type = value.getClass();
            }
        }
        return type;
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext context, Object base) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }
}
