<script>
    let count = 0;

    function handleClick() {
        count += 1;
    }
</script>


<h1>Current Count is {count}</h1>

<button on:click={handleClick}>Click</button>
