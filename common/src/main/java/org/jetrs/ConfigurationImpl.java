/* Copyright (c) 2019 JetRS
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * You should have received a copy of The MIT License (MIT) along with this
 * program. If not, see <http://opensource.org/licenses/MIT/>.
 */

package org.jetrs;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

class ConfigurationImpl implements Cloneable, Configuration {
  private final RuntimeType runtimeType;
  private Set<Class<?>> classes;
  private Set<Object> instances;
  private Map<String,Object> properties;

  ConfigurationImpl(final Application application) {
    this.runtimeType = RuntimeType.SERVER;
    this.classes = application.getClasses();
    this.instances = application.getSingletons();
    this.properties = application.getProperties();
  }

  ConfigurationImpl() {
    this.runtimeType = RuntimeType.CLIENT;
  }

  @Override
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  @Override
  public Map<String,Object> getProperties() {
    return properties == null ? properties = new HashMap<>() : properties;
  }

  @Override
  public Object getProperty(final String name) {
    return getProperties().get(name);
  }

  @Override
  public Collection<String> getPropertyNames() {
    return getProperties().keySet();
  }

  @Override
  public boolean isEnabled(final Feature feature) {
    return isRegistered(feature);
  }

  @Override
  public boolean isEnabled(final Class<? extends Feature> featureClass) {
    return isRegistered(featureClass);
  }

  @Override
  public boolean isRegistered(final Object component) {
    return components != null && components.contains(component);
  }

  private Components components;

  final Components getOrCreateComponents() {
    return components == null ? components = new Components() : components;
  }

  final Components getComponents() {
    return components;
  }

  @Override
  public boolean isRegistered(final Class<?> componentClass) {
    return components != null && components.contains(componentClass);
  }

  @Override
  public Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    return components == null ? Collections.EMPTY_MAP : components.getContracts(componentClass);
  }

  @Override
  public Set<Class<?>> getClasses() {
    return classes != null ? classes : components == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(components.classes());
  }

  @Override
  public Set<Object> getInstances() {
    return instances != null ? instances : components == null ? Collections.EMPTY_SET : Collections.unmodifiableSet(components.instances());
  }

  @Override
  public ConfigurationImpl clone() {
    try {
      final ConfigurationImpl clone = (ConfigurationImpl)super.clone();
      clone.components = components.clone();
      if (classes != null)
        clone.classes = new HashSet<>(classes);

      if (instances != null)
        clone.instances = new HashSet<>(instances);

      if (properties != null)
        clone.properties = new HashMap<>(properties);

      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}