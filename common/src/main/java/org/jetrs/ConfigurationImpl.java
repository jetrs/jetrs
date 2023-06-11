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

import java.io.Serializable;
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

class ConfigurationImpl implements Cloneable, Configuration, Serializable {
  private final RuntimeType runtimeType;
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

  private Set<Feature> features;

  Set<Feature> features() {
    return features == null ? features = new HashSet<>() : features;
  }

  @Override
  public boolean isEnabled(final Feature feature) {
    return features().contains(feature);
  }

  @Override
  public boolean isEnabled(final Class<? extends Feature> featureClass) {
    final Set<Feature> features = features();
    if (features.size() > 0)
      for (final Feature feature : features) // [S]
        if (featureClass.isInstance(feature))
          return true;

    return false;
  }

  @Override
  public boolean isRegistered(final Object component) {
    return components().contains(component);
  }

  private ComponentSet components;

  ComponentSet components() {
    return components == null ? components = new ComponentSet() : components;
  }

  @Override
  public boolean isRegistered(final Class<?> componentClass) {
    return components().contains(componentClass);
  }

  @Override
  public Map<Class<?>,Integer> getContracts(final Class<?> componentClass) {
    return components().getContracts(componentClass);
  }

  private Set<Class<?>> classes;

  @Override
  public Set<Class<?>> getClasses() {
    return classes == null ? classes = Collections.unmodifiableSet(components().classes()) : classes;
  }

  private Set<Object> instances;

  @Override
  public Set<Object> getInstances() {
    return instances == null ? instances = Collections.unmodifiableSet(components().instances()) : instances;
  }

  @Override
  public ConfigurationImpl clone() {
    try {
      final ConfigurationImpl clone = (ConfigurationImpl)super.clone();
      clone.components = components.clone();
      clone.classes = null;
      clone.instances = null;
      return clone;
    }
    catch (final CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }
}