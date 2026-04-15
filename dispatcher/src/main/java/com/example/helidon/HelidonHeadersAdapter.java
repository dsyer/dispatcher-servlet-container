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
package com.example.helidon;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;

import io.helidon.http.Header;
import io.helidon.http.HeaderNames;
import io.helidon.http.Headers;
import io.helidon.http.WritableHeaders;

class HelidonHeadersAdapter implements MultiValueMap<String, String> {

	private final Headers headers;

	private final WritableHeaders<?> writableHeaders;

	HelidonHeadersAdapter(Headers headers) {
		this.headers = headers;
		this.writableHeaders = headers instanceof WritableHeaders<?> writable ? writable : null;
	}

	@Override
	public String getFirst(String key) {
		return this.headers.first(HeaderNames.create(key)).orElse(null);
	}

	@Override
	public void add(String key, String value) {
		assertWritable();
		this.writableHeaders.add(HeaderNames.create(key), value);
	}

	@Override
	public void addAll(String key, List<? extends String> values) {
		for (String value : values) {
			add(key, value);
		}
	}

	@Override
	public void addAll(MultiValueMap<String, String> values) {
		for (Entry<String, List<String>> entry : values.entrySet()) {
			addAll(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void set(String key, String value) {
		assertWritable();
		this.writableHeaders.set(HeaderNames.create(key), value);
	}

	@Override
	public void setAll(Map<String, String> values) {
		for (Entry<String, String> entry : values.entrySet()) {
			set(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public Map<String, String> toSingleValueMap() {
		Map<String, String> singleValueMap = CollectionUtils.newLinkedHashMap(size());
		for (String key : keySet()) {
			singleValueMap.put(key, getFirst(key));
		}
		return singleValueMap;
	}

	@Override
	public int size() {
		return this.headerNames().size();
	}

	@Override
	public boolean isEmpty() {
		return this.headers.size() == 0;
	}

	@Override
	public boolean containsKey(Object key) {
		if (key instanceof String name) {
			return this.headers.contains(HeaderNames.create(name));
		}
		return false;
	}

	@Override
	public boolean containsValue(Object value) {
		if (value instanceof String headerValue) {
			return this.headers.stream().anyMatch(header -> header.allValues().contains(headerValue));
		}
		return false;
	}

	@Override
	public List<String> get(Object key) {
		if (key instanceof String name) {
			if (!this.headers.contains(HeaderNames.create(name))) {
				return null;
			}
			return this.headers.get(HeaderNames.create(name)).allValues();
		}
		return null;
	}

	@Override
	public List<String> put(String key, List<String> value) {
		List<String> previous = get(key);
		assertWritable();
		this.writableHeaders.remove(HeaderNames.create(key));
		for (String current : value) {
			this.writableHeaders.add(HeaderNames.create(key), current);
		}
		return previous;
	}

	@Override
	public List<String> remove(Object key) {
		if (key instanceof String name) {
			List<String> previous = get(name);
			assertWritable();
			this.writableHeaders.remove(HeaderNames.create(name));
			return previous;
		}
		return null;
	}

	@Override
	public void putAll(Map<? extends String, ? extends List<String>> map) {
		for (Entry<? extends String, ? extends List<String>> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public void clear() {
		assertWritable();
		this.writableHeaders.clear();
	}

	@Override
	public Set<String> keySet() {
		return this.headerNames();
	}

	@Override
	public Collection<List<String>> values() {
		return keySet().stream().map(this::get).toList();
	}

	@Override
	public Set<Entry<String, List<String>>> entrySet() {
		return keySet().stream()
				.map(name -> new AbstractMap.SimpleImmutableEntry<>(name, get(name)))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	@Override
	public String toString() {
		return HttpHeaders.formatHeaders(this);
	}

	private Set<String> headerNames() {
		Set<String> keys = new LinkedHashSet<>(this.headers.size());
		Set<String> lowerCaseNames = new LinkedHashSet<>(this.headers.size());
		for (Header header : this.headers) {
			String name = header.name();
			String lowerCaseName = name.toLowerCase(Locale.ROOT);
			if (lowerCaseNames.add(lowerCaseName)) {
				keys.add(name);
			}
		}
		return keys;
	}

	private void assertWritable() {
		if (this.writableHeaders == null) {
			throw new UnsupportedOperationException("Header collection is read-only");
		}
	}

}