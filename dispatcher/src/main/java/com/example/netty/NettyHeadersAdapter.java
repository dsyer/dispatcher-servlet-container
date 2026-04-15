/*
 * Copyright 2026-current the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.netty;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

class NettyHeadersAdapter implements MultiValueMap<String, String> {

	private final io.netty.handler.codec.http.HttpHeaders headers;

	NettyHeadersAdapter(io.netty.handler.codec.http.HttpHeaders headers) {
		this.headers = headers;
	}

	@Override
	public String getFirst(String key) {
		return this.headers.get(key);
	}

	@Override
	public void add(String key, String value) {
		this.headers.add(key, value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		this.headers.add(key, values);
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		values.forEach(this::addAll);
	}

	@Override
	public void set(String key, String value) {
		this.headers.set(key, value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		values.forEach(this::set);
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = new LinkedHashMap<>();
		for (String key : keySet()) {
			singleValueMap.put(key, getFirst(key));
		}
		return singleValueMap;
	}

	@Override
	public int size() {
		return keySet().size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return key instanceof String name && this.headers.contains(name);
	}

	@Override
	public boolean containsValue(Object value) {
		return values().stream().anyMatch(values -> values.equals(value));
	}

	@Override
	public List<String> get(Object key) {
		if (!(key instanceof String name) || !this.headers.contains(name)) {
			return null;
		}
		return this.headers.getAll(name);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		List<String> previous = get(key);
		this.headers.set(key, value);
		return previous;
	}

	@Override
	public List<String> remove(Object key) {
		if (!(key instanceof String name)) {
			return null;
		}
		List<String> previous = get(name);
		this.headers.remove(name);
		return previous;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		map.forEach(this::put);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public Set<String> keySet() {
		return new LinkedHashSet<>(this.headers.names());
	}

	@Override
	public Collection<List<String>> values() {
		List<List<String>> values = new ArrayList<>();
		for (String key : keySet()) {
			List<String> value = get(key);
			if (value != null) {
				values.add(value);
			}
		}
		return values;
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		Set<Entry<String, List<String>>> entries = new LinkedHashSet<>();
		for (String key : keySet()) {
			List<String> values = get(key);
			if (values != null) {
				entries.add(new java.util.AbstractMap.SimpleEntry<>(key, values));
			}
		}
		return entries;
	}

	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}

}