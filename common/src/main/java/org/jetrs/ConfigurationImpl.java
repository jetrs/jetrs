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
import java.util.Map;
import java.util.Set;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

final class ConfigurationImpl implements Cloneable, Configuration {
  private final RuntimeType runtimeType;
  private Components components;
  private Map<String,Object> properties;
  private Set<Class<?>> classes;
  private Set<Object> instances;

  ConfigurationImpl(final Components components, final Map<String,Object> properties) {
    this.runtimeType = RuntimeType.SERVER;
    this.components = components;
    this.properties = properties;
  }

  ConfigurationImpl(final Configuration configuration) {
    this.runtimeType = configuration.getRuntimeType();
    this.components = new Components(configuration);
    final Map<String,Object> properties = configuration.getProperties();
    this.properties = properties == null ? null : new HashMap<>(properties);
  }

  ConfigurationImpl() {
    this.runtimeType = RuntimeType.CLIENT;
  }

  final Components getOrCreateComponents() {
    return components == null ? components = new Components() : components;
  }

  final Components getComponents() {
    return components;
  }

  Map<String,Object> getOrCreateProperties() {
    return properties == null ? properties = new HashMap<>() : properties;
  }

  @Override
  public Map<String,Object> getProperties() {
    return properties == null ? Collections.EMPTY_MAP : Collections.unmodifiableMap(properties);
  }

  @Override
  public Object getProperty(final String name) {
    return properties == null ? null : properties.get(name);
  }

  @Override
  public Collection<String> getPropertyNames() {
    return properties == null ? Collections.EMPTY_LIST : properties.keySet();
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
    return classes != null ? classes : components != null ? classes = components.classes() : Collections.EMPTY_SET;
  }

  @Override
  public Set<Object> getInstances() {
    return instances != null ? instances : components != null ? instances = components.instances() : Collections.EMPTY_SET;
  }

  @Override
  public RuntimeType getRuntimeType() {
    return runtimeType;
  }

  @Override
  public ConfigurationImpl clone() {
    try {
      final ConfigurationImpl clone = (ConfigurationImpl)super.clone();
      if (components != null)
        clone.components = components.clone();

      if (properties != null)
        clone.properties = new HashMap<>(properties);

      clone.classes = null;
      clone.instances = null;
      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}