# Messenger-with-ordering-guarantees.
<p>
A simple android based messenger with provides FIFO, causal and total ordering guarantees for messages. This kind of guarantees are used by common messenger services to ensure message ordering. I have implemented ISIS algorithm to provide ordering guarantees.
</p>
<p>
There is at most one node failure in the middle of conversation. This implementation ensures that even under failed node scenario, other nodes do not wait for responses/proposals from the failed node. In other words, even when a node is crashed, messages do not stall and still provide FIFO total ordering.
</p>
<p>
<a href="https://cse.buffalo.edu/~stevko/courses/cse486/spring13/lectures/12-multicast2.pdf">Click here for the detailed ISIS algorithm</a>
</p>
