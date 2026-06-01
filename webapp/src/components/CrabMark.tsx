type Props = {
  size?: number
  className?: string
}

export default function CrabMark({ size = 32, className }: Props) {
  return (
    <img
      src="/brand/crab-mark.png"
      alt="Crab Do"
      width={size}
      height={size}
      className={className}
      draggable={false}
    />
  )
}
