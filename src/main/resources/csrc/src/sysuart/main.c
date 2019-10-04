/**
 * @file main.c
 * @brief Main program for sanity checking to SysUart.
**/

/* @name UART register address
 * @{ */
#define UART_RX_FIFO 0x8000
#define UART_TX_FIFO 0x8004
#define UART_STAT    0x8008
#define UART_CTRL    0x800c
/* @} */

/**
 * @fn void puts(char c)
 * @brief Put character.
 * @param[in] c Character to send UART.
 */
void puts(char c)
{
    *((char*)UART_TX_FIFO) = c;
}

/**
 * @fn Main
 */
int main()
{
    char *str = "Hello, World!\r\n";

    while (*str != '\0') {
        puts(*str++);
    }

    return 0;
}
