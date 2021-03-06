// Copyright (c) Microsoft. All rights reserved.
// Licensed under the MIT license. See License.txt in the project root.

package com.microsoft.alm.common.utils;

import org.apache.commons.lang.StringUtils;

import java.util.List;

/**
 * Checks for illegal arguments and throws IllegalArgumentException if found
 */
public class ArgumentHelper {
    private static final String EMPTY_ARG_MSG = "%s is empty";

    public static void checkNotNull(final Object arg, final String argName) {
        if (arg == null) {
            throw new IllegalArgumentException(argName);
        }
    }

    public static void checkNotNullOrEmpty(final List arg, final String argName) {
        checkNotNull(arg, argName);

        if (arg.isEmpty()) {
            throw new IllegalArgumentException(String.format(EMPTY_ARG_MSG, argName));
        }
    }

    public static void checkNotEmptyString(final String arg) {
        if (StringUtils.isEmpty(arg)) {
            throw new IllegalArgumentException(String.format(EMPTY_ARG_MSG, arg));
        }
    }
}
