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

#define UART_TX_FIFO_FULL 0x1 << 3

/**
 * @fn void puts(char c)
 * @brief Put character.
 * @param[in] c Character to send UART.
 */
void puts(char c)
{
    volatile unsigned char stat = *((char*)UART_STAT);
    while ((stat & UART_TX_FIFO_FULL) == UART_TX_FIFO_FULL) {
        stat = *((char*)UART_STAT);
    }

    *((char*)UART_TX_FIFO) = c;
}

/**
 * @fn Main
 */
int main()
{
    char *str = "Hello, World! Hello, World!\r\n";

    while (*str != '\0') {
        puts(*str++);
    }

    return 0;
}
