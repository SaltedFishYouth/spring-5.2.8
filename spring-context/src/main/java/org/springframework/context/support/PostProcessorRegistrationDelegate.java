/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}


	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		//已经执行过的bfpp beanName
		Set<String> processedBeans = new HashSet<>();

		//条件成立 说明 当前bf 是bd 注册中心。bd 全部注册到 bf内
		if (beanFactory instanceof BeanDefinitionRegistry) {
			//bf 转换成 bdRegistry
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

			//规整的 普通的 postProcessor
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			//BeanDefinitionRegistryPostProcessor set
			//BeanDefinitionRegistryPostProcessor 在 BeanFactoryPostProcessor 的基础上 提供了个 添加bdRegistry 的方法
			//手动硬编码注册一些b d
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

			//处理ApplicationContext上面 硬编码注册的一些 bfpp处理器
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					//这里调用 可以向 bf 内部 添加bd信息
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					//添加到集合
					registryProcessors.add(registryProcessor);
				}
				else {
					//非 registryProcessor 类型 放入普通集合 后面统一执行
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			//临时的 当前阶段的 RegistryProcessor 后处理器集合
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			//使用bf 获取 BeanDefinitionRegistryPostProcessor 类型的全部beanName
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			//处理每个beanName
			for (String ppName : postProcessorNames) {
				//每个bean 是否实现了 主排序接口PriorityOrdered  如果实现类 就让该 bfrpp 添加到 currentRegistryProcessors
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//添加到 当前阶段的 registryProcessors 集合
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					//接下来 马上执行 bfrpp 的 Registry 相关方法 所以将beanName 添加到processedBeans 表示已经执行
					processedBeans.add(ppName);
				}
			}

			//进行排序 根据主排序接口PriorityOrdered 的ordered值 升序 排序
			sortPostProcessors(currentRegistryProcessors, beanFactory);

			//添加到 registryProcessors 中
			registryProcessors.addAll(currentRegistryProcessors);
			//遍历 调用 bdrpp 的postProcessBeanDefinitionRegistry 向bf 注册bd信息
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			//清空集合
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			//这里的逻辑和上面的一样
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				//普通排序
				//!processedBeans.contains(ppName) 没有被执行过
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();


			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			//控制是否接着循环  因为循环内 就是查找并 执行bdrpp后处理器
			//接口执行后 会向bf 中注册 bd,但是再注册的 bd 也有可能还是 bdrpp 所以就 改为true 接着循环 只有没注册了 才退出
			boolean reiterate = true;
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				//排序
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				//添加到 registry集合中
				registryProcessors.addAll(currentRegistryProcessors);
				//执行
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				//当前阶段集合中清空
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			//所有的registry 都继承了 BeanFactoryPostProcessor 接口，上面只执行了 添加bd信息的方法， 还有postProcessBeanFactory 方法没执行 这里统一执行
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			//执行硬编码 提供的 BeanFactoryPostProcessor实现类 不是registry 类型的
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			//忽视
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!

		//上面 代码 处理了 硬编码 提供的 bfpp 和bdrpp 这两种
		//这里提取容器内 注册的 bfpp 类型 的beanName 集合

		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		//主排序后处理器集合
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		//次排序后处理器集合
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//未实现排序接口的 后处理器集合
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		//筛选未执行的 bfpp ,根据实现的排序接口放入 上面三集合
		for (String ppName : postProcessorNames) {

			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		//排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		//次排序的  根据名字 获取到BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		//排序
		sortPostProcessors(orderedPostProcessors, beanFactory);
		//执行
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		//无排序的  根据名字 获取到BeanFactoryPostProcessor
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		//执行
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}

	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {

		//获取后处理器 beanNames
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);

		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.

		//后处理器数量
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;

		//BeanPostProcessorChecker 检查 创建bean 实例时 后处理器 是否全部注册完
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.

		//存储主排序接口 的后处理器集合
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		//存储实现了 MergedBeanDefinitionPostProcessor 的后处理器
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		//存储实现了 次排序 接口的后处理器 name 集合
		List<String> orderedPostProcessorNames = new ArrayList<>();
		//存储未实现排序接口的 后处理器 name 集合
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();

		for (String ppName : postProcessorNames) {

			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				//实现了主排序的 添加到 主排序集合中
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				//次排序的加到次排序集合
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.

		//注册实现了 主排序接口的 后处理器 开始  （实例化过程中 不同阶段 执行）
		//1、排序
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		//2、注册 内部 bf 先remove 再添加 保证不会重复
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);


		// Next, register the BeanPostProcessors that implement Ordered.

		//这里也是 筛选 添加进集合 然后排序 注册 和上面主排序的一样
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
		for (String ppName : orderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			orderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.

		//这里也是 筛选 添加进集合 然后排序 注册 和上面主排序的一样 就是少了排序 的步骤
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
		for (String ppName : nonOrderedPostProcessorNames) {
			BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		}
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);


		// Finally, re-register all internal BeanPostProcessors.

		//排序内部处理器
		sortPostProcessors(internalPostProcessors, beanFactory);
		//再次注册 bf 内部处理器，确保 MergedBeanDefinitionPostProcessor 类型的后处理器 在后处理器list 的末尾
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).

		//重新 注册ApplicationListenerDetector 后处理器
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		// Nothing to sort?
		if (postProcessors.size() <= 1) {
			return;
		}
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			//条件一：当前bean 普通bean 不是后处理器
			//条件二：不是spring架构的bean 是用户的bean
			//条件三：如果已经注册的后处理数量 小于 后处理器 总数，说明 bean 创建阶段 后处理没加载完
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
