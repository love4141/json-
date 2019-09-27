package com.yyf.myjson;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Myjson {
	// 对象序列化
		public String toJson(Object o) {
			StringBuilder builder = new StringBuilder();
			String str;
			if(o == null) {
				return null;
			}
			if(o.getClass().isArray() || List.class.isAssignableFrom(o.getClass())) {
				
				str = listTransform(o, null);
			}
			else if(Map.class.isAssignableFrom(o.getClass()))
				str = mapTransform(o, null);
			else if(isCustomed(o.getClass()))
				str = cusObjTransform(o);
			else {
				if(o.getClass() == Character.class)
					str = String.format("'%s'", o);
				else if(o.getClass() == Integer.class) {
					str = String.format("%s", o);
				}
				else if(o.getClass() == Boolean.class) {
					str = String.format("%s", o);
				}
				else {
					str = String.format("\"%s\"", o);
				}
			}
			builder.append(str);
//			System.out.println(builder);
			return builder.toString();

		}
		
			// 类型转换
		private String cusObjTransform(Object o) {
			// TODO Auto-generated method stub
			// 获得类型
			Class clazz = o.getClass();
			Field[] fs = clazz.getDeclaredFields();
			Method[] ms = clazz.getMethods();
			
			
			
			StringBuilder builder = new StringBuilder("{");
			for (int i = 0; i < fs.length; i++) {
				Field f = fs[i];
			
				String name = f.getName();
				String name2 = "get" + name.substring(0, 1).toUpperCase() + name.substring(1);
				System.out.println(name2);
				try {
					// 获得 getxxx 方法
					Method method = clazz.getDeclaredMethod(name2);
					// 调用方法获得属性值
					Object value = method.invoke(o);
					// 字符串的拼接
					
					if(value == null) {
						builder.append(String.format("\"%s\":%s,", name, value));
						continue;
					}
					
					// 获得对应字段的对象
					f.setAccessible(true);
					Object obj = f.get(o);

					// 判断获得的字段的类型是否是Map的子类
					if(Map.class.isAssignableFrom(f.getType())) {
						builder.append(mapTransform(obj, name));
						builder.append(",");
						continue;
					}
					
					// 判断获得的字段的类型是否是List的子类 或 数组
					if(List.class.isAssignableFrom(f.getType()) || f.getType().isArray()) {
						
						builder.append(listTransform(obj, name));
						builder.append(",");
						continue;
					}
					// 自定义Object类
					if(isCustomed(f.getType())) {
						
						builder.append(cusObjTransform(obj));
						builder.append(",");
						continue;
					}
					
					builder.append(baseTransform(obj, name, value));
				} catch (NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
			}
			builder.append("}");
			
			// 最外围是对象开始
			if(builder.charAt(builder.length() - 1) == '}') {
				builder.deleteCharAt(builder.length() - 2);
			} else {
				builder.deleteCharAt(builder.length() - 1);
			}
			return builder.toString();
		}

		// 基本数据类型转换
		private String baseTransform(Object obj, String name, Object value) {
			StringBuilder builder = new StringBuilder();
			if(obj.getClass() == char.class || obj.getClass() == Character.class)
				builder.append(String.format("\"%s\":'%s',", name, value));

			else if(obj.getClass()!= String.class) {
				builder.append(String.format("\"%s\":%s,", name, value));
			}
			else {
				builder.append(String.format("\"%s\":\"%s\",", name, value));
			}
			return builder.toString();
		}
		
		
		
		// 列表类型、数组转换
		private String listTransform(Object obj, String name) {
			// TODO Auto-generated method stub
			
			// 获取列表
			StringBuilder builder = new StringBuilder();
			System.out.println(name == null);
			if(name == null)
				builder.append("[");
			else
				builder.append(String.format("\"%s\":[", name));
			// 列表里可能是列表、图、数组
			Object[] o;
			if(!obj.getClass().isArray()) {
				List<Object> list = (List)obj;
				o = list.toArray();
			} else {
				
				// 获取数组的值并转化为Object数组
				int len = Array.getLength(obj);
				System.out.println(len);
				o = new Object[len];
				for (int i = 0; i < len; i++) {
					o[i] = Array.get(obj, i);
				}
			}
			for (int i = 0; i < o.length; i++) {
				if(o[i] == null) {
					continue;
				} else if(List.class.isAssignableFrom(o[i].getClass()) || Map.class.isAssignableFrom(o[i].getClass())
						|| o[i].getClass().getClassLoader() != null || o[i].getClass().isArray()) {
					
					// 自定义类型 列表 图 数组 不能直接赋值
					builder.append(cusObjTransform(o[i]));
					builder.append(",");
					continue;
				} else {
					if(o[i].getClass() == Character.class)
						builder.append(String.format("char:'%s',", o[i]));
					
					else if(o[i].getClass() == Boolean.class) {
						builder.append(String.format("boolean:%s", o[i]));
					}
					else if(o[i].getClass() == Integer.class) {
						builder.append(String.format("int:%s,", o[i]));
					} 
					else {
						builder.append(String.format("string:\"%s\",", o[i]));
					}
				}
				
				
			}
			
			builder.append("]");
			if(builder.charAt(builder.length() - 2) == ',')
				builder.deleteCharAt(builder.length() - 2);
			return builder.toString();
		}

		
		
		
		// 图类型转换
		private String mapTransform(Object obj, String name) {
			// TODO Auto-generated method stub
			StringBuilder builder = new StringBuilder();
			if(name == null) 
				builder.append("{");
			else
				builder.append(String.format("\"%s\":{", name));
			Map<Object, Object> map = (HashMap<Object, Object>)obj;
			// 获得键集合
			Set<Object> keySet = map.keySet();
			// 获得迭代器
			Iterator<Object> it = keySet.iterator();
			while(it.hasNext()) {
				Object k = it.next();
				Object v = map.get(k);
				builder.append(String.format("\"%s\":", k));
				if(List.class.isAssignableFrom(v.getClass()) || Map.class.isAssignableFrom(v.getClass())
						|| v.getClass().getClassLoader() != null || v.getClass().isArray()) {
					builder.append(cusObjTransform(v));
					builder.append(",");
					continue;
				}
				if(v.getClass() == char.class || v.getClass() == Character.class)
					builder.append(String.format("'%s',", v));

				else if(obj.getClass()!= String.class) {
					builder.append(String.format("%s,", v));
				}
				else {
					builder.append(String.format("\"%s\",", v));
				}
			}
			builder.append(String.format("}"));
			builder.deleteCharAt(builder.length() - 2);
			return builder.toString();
		}

		
			
		
		// 判断是否是自定义类型
		private boolean isCustomed(Class type) {
			// TODO Auto-generated method stub
			if(type.getName().startsWith("java") || type.isPrimitive())
				return false;
			return true;
		}


		
		// JSON格式字符串反序列化成对应对象
		public Object fromJson(String json, Class clazz) {
			return null;
		}
}
