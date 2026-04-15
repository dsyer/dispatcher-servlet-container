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
package com.example.standard;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.util.MultiValueMap;

import com.sun.net.httpserver.Headers;

class StandardHeadersAdapter implements MultiValueMap<String, String> {

	private final Headers headers;

	StandardHeadersAdapter(Headers headers) {
		this.headers = headers;
	}

	@Override
	public String getFirst(String key) {
		return this.headers.getFirst(key);
	}

	@Override
	public void add(String key, String value) {
		this.headers.add(key, value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		for (String value : values) {
			this.headers.add(key, value);
		}
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
		for (String key : this.headers.keySet()) {
			singleValueMap.put(key, this.headers.getFirst(key));
		}
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headers.size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.isEmpty();
	}

	@Override
	public boolean containsKey(Object key) {
		return this.headers.containsKey(key);
	}

	@Override
	public boolean containsValue(Object value) {
		return this.headers.containsValue(value);
	}

	@Override
	public List<String> get(Object key) {
		return this.headers.get(key);
	}

	@Override
	public List<String> put(String key, List<String> value) {
		return this.headers.put(key, value);
	}

	@Override
	public List<String> remove(Object key) {
		return this.headers.remove(key);
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		this.headers.putAll(map);
	}

	@Override
	public void clear() {
		this.headers.clear();
	}

	@Override
	public java.util.Set<String> keySet() {
		return this.headers.keySet();
	}

	@Override
	public java.util.Collection<List<String>> values() {
		return this.headers.values();
	}

	@Override
	public java.util.Set<Entry<String, List<String>>> entrySet() {
		return this.headers.entrySet();
	}

	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}

}