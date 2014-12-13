/*
 * Copyright (c)
 */
package io.redis.jedis;

/**
 * Redis service for Jedis framework.
 *
 * @author huagang.li
 * @since 1.0
 */
public interface JedisService {

	/*
	 * Lists at http://redis.io/commands#list
	 * 
	 * List是一个双向链表，支持双向的Pop/Push。
	 * 所有操作都是O(1)的好孩子，可以当Message Queue来用。
	 * 任务队列系统Resque是其典型应用。
	 */
	/**
	 * Returns the length of the list stored at key.
	 * <p>
	 * If key does not exist, it is interpreted as an empty list and {@code 0} is returned.
	 * An error is returned when the value stored at key is not a list.
	 * <p>
	 * Time complexity: O(1)<br>
	 * LLEN command at http://redis.io/commands/llen
	 *
	 * @param key
	 * @return the length of the list at key.
	 */
	long llen(String key);
	
	// 江湖规矩一般从左端Push，右端Pop——LPush/RPop
	/**
	 * Insert all the specified values at the head of the list stored at key. (插到链表头部)
	 * <p>
	 * If key does not exist, it is created as empty list before performing the push operations. 
	 * When key holds a value that is not a list, an error is returned.
	 * <p>
	 * It is possible to push multiple elements using a single command call just
	 * specifying multiple arguments at the end of the command. 
	 * Elements are inserted one after the other to the head of the list, 
	 * from the leftmost element to the rightmost element. 
	 * So for instance the command LPUSH mylist a b c will result into a list 
	 * containing c as first element, b as second element and a as third element.
	 * <p>
	 * Time complexity: O(1)<br>
	 * LPUSH command at http://redis.io/commands/lpush
	 * 
	 * @param key
	 * @param string
	 * @return the length of the list after the push operations.
	 */
	long lpush(String key, String... string);
	
	/**
	 * Removes and returns the last element of the list stored at key. (移除并返回链表的最后一个元素)
	 * <p>
	 * Time complexity: O(1)<br>
	 * RPOP command at http://redis.io/commands/rpop
	 *
	 * @param key
	 * @return the value of the last element, or {@code null} when key does not exist.
	 */
	String rpop(String key);
	
	// LTrim，限制List的大小（比如只保留最新的20条消息）
	/**
	 * Trim an existing list so that it will contain only the specified range of elements specified.
	 * <p>
	 * Both start and stop are zero-based indexes, 
	 * where 0 is the first element of the list (the head), 1 the next element and so on.
	 * <p>
	 * For example: LTRIM foobar 0 2 will modify the list stored at foobar 
	 * so that only the first three elements of the list will remain.
	 * <p>
	 * start and end can also be negative numbers indicating offsets from the end of the list, 
	 * where -1 is the last element of the list, -2 the penultimate element and so on.<br>
	 * <font color="red">注意：</font>头、尾索引的区别！
	 * <p>
	 * Out of range indexes will not produce an error: 
	 * if start is larger than the end of the list, or start > end, the result will be an empty list 
	 * (which causes key to be removed). 
	 * If end is larger than the end of the list, Redis will treat it like the last element of the list.
	 * <p>
	 * A common use of LTRIM is together with LPUSH / RPUSH. For example:
	 * <pre>
	 * LPUSH mylist someelement
	 * LTRIM mylist 0 99
	 * </pre>
	 * This pair of commands will push a new element on the list, 
	 * while making sure that the list will not grow larger than 100 elements. 
	 * This is very useful when using Redis to store logs for example. 
	 * It is important to note that when used in this way LTRIM is an O(1) operation 
	 * because in the average case just one element is removed from the tail of the list.
	 * <p>
	 * Time complexity: O(N) where N is the number of elements to be removed by the operation.<br>
	 * LTRIM command at http://redis.io/commands/ltrim
	 *
	 * @param key
	 * @param start
	 * @param end
	 * @return Simple string reply ({@code "OK"})
	 */
	String ltrim(String key, long start, long end);

}
