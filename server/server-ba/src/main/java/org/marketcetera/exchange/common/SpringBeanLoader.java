/*
 * CATSBF CCFEA Algorithmic Trading Strategy Backtesting Framework
 * Copyright (C) 2011 Daniel Schiermer
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.marketcetera.exchange.common;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

/**
 * Spring bean loader responsible for loading spring beans.
 * 
 * @author <a href="mailto:catsbf@dasch.dk">Daniel Schiermer</a>
 * @version 0.1
 *
 */
public class SpringBeanLoader {
	private static BeanFactory beanFactory;

	public SpringBeanLoader() {
		//Open the config file as system resource
		Resource resource = new FileSystemResource("config/SpringConfig.xml");

		//Create the bean factory
		beanFactory = new XmlBeanFactory(resource);
	}

	public Object getBean(String beanId) {
		Object bean = beanFactory.getBean(beanId);

		return bean;
	}
}