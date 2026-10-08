document.addEventListener('DOMContentLoaded', () => {
    const yearNode = document.getElementById('year');
    if (yearNode) {
        yearNode.textContent = new Date().getFullYear();
    }

    const aiButton = document.querySelector('.chat-input button');
    if (aiButton) {
        aiButton.addEventListener('click', () => {
            const input = document.querySelector('.chat-input span');
            if (input) {
                input.textContent = 'AI is ready to help.';
            }
        });
    }
});
