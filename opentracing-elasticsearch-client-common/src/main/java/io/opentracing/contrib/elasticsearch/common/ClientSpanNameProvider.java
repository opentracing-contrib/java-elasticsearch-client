/*
 * Copyright 2017-2018 The OpenTracing Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.opentracing.contrib.elasticsearch.common;

import org.apache.http.HttpRequest;

import java.util.function.Function;

/**
 * @author Jordan J Lopez
 * Returns a string to be used as the name of the spans, based on
 * the operation preformed and the record the span is based off of.
 */
public class ClientSpanNameProvider {

    private String regexParameterPattern = "\\?*+$";
    private String regexTaskIDPattern = "task_id:(\\d+)";

    public static Function<HttpRequest, String> REQUEST_METHOD_NAME =
            (request) -> replaceIfNull(request.getRequestLine().getMethod(), "unknown");

    public static Function<HttpRequest, String> PREFIXED_REQUEST_METHOD_NAME(final String prefix) {
        return (request) -> replaceIfNull(prefix, "")
                + replaceIfNull(request.getRequestLine().getMethod(), "unknown");
    }

    /**
     * The following methods are derived from the Elasticsearch http API found here:
     * https://www.elastic.co/guide/en/elasticsearch/reference/5.6/docs.html
     */
    public static Function<HttpRequest, String> REQUEST_TARGET_NAME =
            (request) -> replaceIfNull(standardizeUri(request.getRequestLine().getUri()), "unknown");

    public static Function<HttpRequest, String> PREFIXED_REQUEST_TARGET_NAME(final String prefix) {
        return (request) -> replaceIfNull(prefix, "")
                + replaceIfNull(standardizeUri(request.getRequestLine().getUri()), "unknown");
    }


    public static Function<HttpRequest, String> REQUEST_METHOD_TARGET_NAME =
            (request) -> replaceIfNull(request.getRequestLine().getMethod(), "unknown")
                    + " " + replaceIfNull(standardizeUri(request.getRequestLine().getUri()), "unknown");

    public static Function<HttpRequest, String> PREFIXED_REQUEST_METHOD_TARGET_NAME(final String prefix) {
        return (request) -> replaceIfNull(prefix, "")
                + replaceIfNull(request.getRequestLine().getMethod(), "unknown")
                + " " + replaceIfNull(standardizeUri(request.getRequestLine().getUri()), "unknown");
    }

    private static String replaceIfNull(String input, String replacement) {
        return (input == null) ? replacement : input;
    }

    private static String standardizeUri(String uri) {
        return (uri == null) ? null : uri.replaceAll("\\?.*$", "") // Removes parameters
                .replaceAll("task_id:\\d+", "task_id:\\?") // Replaces unique IDs with "?"
                .replaceAll("/\\d+", "/\\?"); // Replaces unique IDs with "?"
    }
}
