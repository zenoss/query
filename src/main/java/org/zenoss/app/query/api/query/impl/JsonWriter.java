/*
 * Copyright (c) 2013, Zenoss and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Zenoss or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.zenoss.app.query.api.query.impl;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * An extension of the BufferedWriter class that add convience methods to allow
 * the easier writing of JSON objects.
 * 
 * @author David Bainbridge <dbainbridge@zenoss.com>
 */
public class JsonWriter extends BufferedWriter {

	/**
	 * Constructs a JsonWriter that uses the specified writer for the actual
	 * writing of data.
	 * 
	 * @param out
	 *            the writer that will be utilized
	 */
	public JsonWriter(Writer out) {
		super(out);
	}

	/**
	 * Writes a name / "string" value pair in JSON format to the output with an
	 * optionally appended comma
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, String value, boolean appendComma)
			throws IOException {
		write('\"');
		write(name);
		write("\":\"");
		write(value);
		write('\"');
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes a name / "string" value pair in JSON format to the output with no
	 * comma appended
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, String value) throws IOException {
		value(name, value, false);
		return this;
	}

	/**
	 * Writes a name / "boolean" value pair in JSON format to the output with an
	 * optionally appended comma
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, boolean value, boolean appendComma)
			throws IOException {
		write('\"');
		write(name);
		write("\":");
		write(Boolean.toString(value));
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes a name / "boolean" value pair in JSON format to the output with no
	 * comma appended
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, boolean value) throws IOException {
		value(name, value, false);
		return this;
	}

	/**
	 * Writes a name / "long" value pair in JSON format to the output with an
	 * optionally appended comma
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, long value, boolean appendComma)
			throws IOException {
		write('\"');
		write(name);
		write("\":");
		write(Long.toString(value));
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes a name / "long" value pair in JSON format to the output with no
	 * comma appended
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, long value) throws IOException {
		value(name, value, false);
		return this;
	}

	/**
	 * Writes a name / "double" value pair in JSON format to the output with an
	 * optionally appended comma
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, double value, boolean appendComma)
			throws IOException {
		write('\"');
		write(name);
		write("\":");
		write(Double.toString(value));
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes a name / "double" value pair in JSON format to the output with no
	 * comma appended
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, double value) throws IOException {
		value(name, value, false);
		return this;
	}

	/**
	 * Writes a name / "int" value pair in JSON format to the output with an
	 * optionally appended comma
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, int value, boolean appendComma)
			throws IOException {
		write('\"');
		write(name);
		write("\":");
		write(Integer.toString(value));
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes a name / "int" value pair in JSON format to the output with no
	 * comma appended
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, int value) throws IOException {
		value(name, value, false);
		return this;
	}

	/**
	 * Writes a name / "float" value pair in JSON format to the output with an
	 * optionally appended comma
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, float value, boolean appendComma)
			throws IOException {
		write('\"');
		write(name);
		write("\":");
		write(Float.toString(value));
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes a name / "float" value pair in JSON format to the output with no
	 * comma appended
	 * 
	 * @param name
	 *            name of the property
	 * @param value
	 *            value of the property
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter value(String name, float value) throws IOException {
		value(name, value, false);
		return this;
	}

	/**
	 * Writes the starting elements of a named JSON array to the output stream
	 * 
	 * @param name
	 *            name of the array
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter arrayS(String name) throws IOException {
		if (name != null) {
			write('\"');
			write(name);
			write("\":[");
		} else {
			write('[');
		}
		return this;
	}

	/**
	 * Writes the starting elements of a unnamed JSON array to the output stream
	 * 
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter arrayS() throws IOException {
		write('[');
		return this;
	}

	/**
	 * Writes the ending elements of a JSON array to the output stream with an
	 * optional comma
	 * 
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter arrayE(boolean appendComma) throws IOException {
		write(']');
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes the ending elements of a JSON array to the output stream with no
	 * optional comma
	 * 
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter arrayE() throws IOException {
		write(']');
		return this;
	}

	/**
	 * Writes the starting elements of a named JSON object to the output stream
	 * 
	 * @param name
	 *            name of the object
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter objectS(String name) throws IOException {
		if (name != null) {
			write('\"');
			write(name);
			write("\":{");
		} else {
			write('{');
		}
		return this;
	}

	/**
	 * Writes the starting elements of a unnamed JSON object to the output
	 * stream
	 * 
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter objectS() throws IOException {
		write('{');
		return this;
	}

	/**
	 * Writes the ending elements of a JSON object to the output stream with an
	 * optional comma
	 * 
	 * @param appendComma
	 *            determines if a comma will be appended
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter objectE(boolean appendComma) throws IOException {
		write('}');
		if (appendComma) {
			write(',');
		}
		return this;
	}

	/**
	 * Writes the ending elements of a JSON array to the output stream with no
	 * optional comma
	 * 
	 * @return the JsonWriter, so chaining can be utilized
	 * @throws IOException
	 *             when then underlying writer throws an exception
	 */
	public JsonWriter objectE() throws IOException {
		write('}');
		return this;
	}
}
